package org.jucamdonaciones.swgdsjucambackend.controller;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jucamdonaciones.swgdsjucambackend.model.Donacion;
import org.jucamdonaciones.swgdsjucambackend.model.Donacion.Estado;
import org.jucamdonaciones.swgdsjucambackend.model.Suscripcion;
import org.jucamdonaciones.swgdsjucambackend.model.Suscripcion.EstadoSuscripcion;
import org.jucamdonaciones.swgdsjucambackend.model.Suscriptor;
import org.jucamdonaciones.swgdsjucambackend.payload.WebhookPayload;
import org.jucamdonaciones.swgdsjucambackend.repository.DonacionRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.SuscripcionRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.SuscriptorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/webhook")
public class WebhookSuscripcionesController {

    private static final String AUTH_HEADER = "Basic NTYxZTY4ODgtNDJlNi00OTNhLTg3ODQtZDgyNDVlMTE3YTlkOmFmMzljYWYyLTgyMGYtNDAzMC1hYWU5LTlkNGU2ZDE2ZmI0YQ==";

    @Autowired
    private SuscriptorRepository suscriptorRepository;

    @Autowired
    private SuscripcionRepository suscripcionRepository;

    @Autowired
    private DonacionRepository donacionRepository;

    @PostMapping("/suscripciones")
    public ResponseEntity<?> recibirWebhookSuscripciones(@RequestBody WebhookPayload payload) {
        System.out.println("Webhook de suscripciones recibido: " + payload);
        
        // Solo procesamos webhooks cuyo origin sea "invoice" y event_type "update"
        if (!"invoice".equalsIgnoreCase(payload.getOrigin()) ||
            !"update".equalsIgnoreCase(payload.getEvent_type())) {
            return ResponseEntity.ok("Webhook ignorado: origen o event_type no aplicable");
        }
        
        String invoiceId = payload.getId();
        
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_HEADER);
        
        // Consultar el invoice en la API de Clip
        String invoiceUrl = "https://api.payclip.com/invoices/" + invoiceId;
        HttpEntity<String> invoiceEntity = new HttpEntity<>(headers);
        ResponseEntity<Map> invoiceResponseEntity;
        try {
            invoiceResponseEntity = restTemplate.exchange(invoiceUrl, HttpMethod.GET, invoiceEntity, Map.class);
        } catch (Exception e) {
            System.err.println("Error al consultar invoice: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al consultar invoice");
        }
        
        Map<String, Object> invoiceResponse = invoiceResponseEntity.getBody();
        if (invoiceResponse == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Respuesta vacía del invoice");
        }
        System.out.println("Respuesta de invoice: " + invoiceResponse);
        
        String status = (String) invoiceResponse.get("status");
        if (status == null || !"paid".equalsIgnoreCase(status)) {
            return ResponseEntity.ok("Invoice status no es 'paid', sin acción");
        }
        
        // Extraer el subscription_id (ID de la suscripción en Clip)
        String clipSubscriptionId = (String) invoiceResponse.get("subscription_id");
        if (clipSubscriptionId == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se encontró subscription_id en el invoice");
        }
        
        // Para obtener el método de pago real, primero extraemos el payment_request_id del invoice
        String prId = (String) invoiceResponse.get("payment_request_id");
        String metodoPago = "desconocido";
        if (prId != null) {
            // Consultar el checkout correspondiente al payment_request_id
            String checkoutUrlForPR = "https://api.payclip.com/v2/checkout/" + prId;
            HttpEntity<String> checkoutEntityForPR = new HttpEntity<>(headers);
            try {
                ResponseEntity<Map> checkoutResponseEntityForPR = restTemplate.exchange(checkoutUrlForPR, HttpMethod.GET, checkoutEntityForPR, Map.class);
                Map<String, Object> checkoutResponseForPR = checkoutResponseEntityForPR.getBody();
                if (checkoutResponseForPR != null) {
                    String receiptNo = (String) checkoutResponseForPR.get("receipt_no");
                    if (receiptNo != null) {
                        // Calcular from y to para el día en curso
                        LocalDateTime now = LocalDateTime.now();
                        LocalDateTime fromDate = now.toLocalDate().atStartOfDay();
                        LocalDateTime toDate = fromDate.plusDays(1);
                        String fromStr = java.time.Instant.from(fromDate.atZone(ZoneOffset.UTC)).toString();
                        String toStr = java.time.Instant.from(toDate.atZone(ZoneOffset.UTC)).toString();
                        
                        String paymentsUrlForPR = "https://api.payclip.com/payments?from=" + fromStr + "&to=" + toStr + "&receipt_no=" + receiptNo;
                        HttpEntity<String> paymentsEntityForPR = new HttpEntity<>(headers);
                        ResponseEntity<Map> paymentsResponseEntityForPR = restTemplate.exchange(paymentsUrlForPR, HttpMethod.GET, paymentsEntityForPR, Map.class);
                        Map<String, Object> paymentsResponseForPR = paymentsResponseEntityForPR.getBody();
                        if (paymentsResponseForPR != null) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> results = (List<Map<String, Object>>) paymentsResponseForPR.get("results");
                            if (results != null && !results.isEmpty()) {
                                Map<String, Object> paymentDetails = results.get(0);
                                @SuppressWarnings("unchecked")
                                Map<String, Object> pm = (Map<String, Object>) paymentDetails.get("payment_method");
                                if (pm != null && pm.get("type") != null) {
                                    metodoPago = pm.get("type").toString();
                                }
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error al consultar checkout/payments: " + e.getMessage());
            }
        }
        
        // Verificar si la suscripción ya está registrada en la BD
        Suscripcion suscripcionExistente = suscripcionRepository.findByClipSubscriptionId(clipSubscriptionId);
        
        // Datos comunes del invoice para el pago
        BigDecimal amount;
        try {
            amount = new BigDecimal(invoiceResponse.get("amount").toString());
        } catch (Exception ex) {
            amount = BigDecimal.ZERO;
        }
        String transaccionId = invoiceId;  // Usamos el ID del invoice como transacción
        LocalDateTime fechaUltimoPago = null;
        if (invoiceResponse.get("paid_at") != null) {
            String paidAtStr = invoiceResponse.get("paid_at").toString();
            fechaUltimoPago = LocalDateTime.parse(paidAtStr.substring(0, 19));
        }
        
        if (suscripcionExistente == null) {
            // Suscripción nueva: consultar la API de Clip para obtener detalles de la suscripción
            String subscriptionUrl = "https://api.payclip.com/subscriptions/" + clipSubscriptionId;
            HttpEntity<String> subscriptionEntity = new HttpEntity<>(headers);
            ResponseEntity<Map> subscriptionResponseEntity;
            try {
                subscriptionResponseEntity = restTemplate.exchange(subscriptionUrl, HttpMethod.GET, subscriptionEntity, Map.class);
            } catch (Exception e) {
                System.err.println("Error al consultar subscription: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al consultar subscription");
            }
            Map<String, Object> subscriptionResponse = subscriptionResponseEntity.getBody();
            if (subscriptionResponse == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Respuesta vacía de subscription");
            }
            System.out.println("Respuesta de subscription: " + subscriptionResponse);
            
            // Registrar al suscriptor usando la información del customer
            @SuppressWarnings("unchecked")
            Map<String, Object> customer = (Map<String, Object>) subscriptionResponse.get("customer");
            if (customer == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se encontró customer en subscription");
            }
            String firstName = (String) customer.get("first_name");
            String lastName = (String) customer.get("last_name");
            String customerEmail = (String) customer.get("email");
            String customerPhone = (String) customer.get("phone");
            
            // Consultar si ya existe un suscriptor con este email y teléfono
            Suscriptor suscriptor = suscriptorRepository.findByEmailAndTelefono(customerEmail, customerPhone);
            if (suscriptor == null) {
                suscriptor = new Suscriptor();
                suscriptor.setNombre(firstName + " " + lastName);
                suscriptor.setEmail(customerEmail);
                suscriptor.setTelefono(customerPhone);
                suscriptor.setConsentimientoDatos(true);
                suscriptor.setFechaRegistro(LocalDateTime.now());
                suscriptor = suscriptorRepository.save(suscriptor);
            }
            
            // Registrar la nueva suscripción
            Suscripcion nuevaSuscripcion = new Suscripcion();
            nuevaSuscripcion.setClipSubscriptionId(clipSubscriptionId);
            nuevaSuscripcion.setSuscriptor(suscriptor);
            // El price_id de la suscripción se usa como servicio_id (convertido a UUID)
            String servicioIdStr = (String) subscriptionResponse.get("price_id");
            nuevaSuscripcion.setServicioId(UUID.fromString(servicioIdStr));
            nuevaSuscripcion.setFechaInicio(LocalDateTime.now());
            String subStatus = (String) subscriptionResponse.get("status");
            if ("active".equalsIgnoreCase(subStatus)) {
                nuevaSuscripcion.setEstado(EstadoSuscripcion.activa);
            } else {
                nuevaSuscripcion.setEstado(EstadoSuscripcion.suspendida);
            }
            nuevaSuscripcion.setMetodoPago(metodoPago);
            nuevaSuscripcion.setTransaccionId(transaccionId);
            nuevaSuscripcion.setFechaUltimoPago(fechaUltimoPago);
            
            // Buscar en el arreglo "invoices" de la suscripción un invoice con status "scheduled" para determinar el próximo pago
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> invoices = (List<Map<String, Object>>) subscriptionResponse.get("invoices");
            LocalDateTime proximoPago = null;
            if (invoices != null) {
                for (Map<String, Object> inv : invoices) {
                    String invStatus = (String) inv.get("status");
                    if ("scheduled".equalsIgnoreCase(invStatus)) {
                        Object dueDateObj = inv.get("due_date");
                        if (dueDateObj != null) {
                            String dueDateStr = dueDateObj.toString();
                            proximoPago = LocalDateTime.parse(dueDateStr.substring(0, 19));
                            break;
                        }
                    }
                }
            }
            nuevaSuscripcion.setProximoPago(proximoPago);
            
            suscripcionExistente = suscripcionRepository.save(nuevaSuscripcion);
            
            // Registrar el pago en la tabla de donaciones (para suscripciones, donador_id es null)
            Donacion donacion = new Donacion();
            donacion.setDonador(null);
            donacion.setMonto(amount);
            donacion.setMetodoPago(metodoPago);
            donacion.setEstado(Estado.exitoso);
            donacion.setTransaccionId(transaccionId);
            donacion.setAnonimato(false);
            donacion.setFrecuencia(Donacion.Frecuencia.única);
            donacion.setSuscripcionId(suscripcionExistente.getId());
            donacionRepository.save(donacion);
            
            return ResponseEntity.ok("Nueva suscripción registrada y pago guardado");
        } else {
            // La suscripción ya existe: solo registrar el pago
            Donacion donacion = new Donacion();
            donacion.setDonador(null);
            donacion.setMonto(amount);
            donacion.setMetodoPago(metodoPago);
            donacion.setEstado(Estado.exitoso);
            donacion.setTransaccionId(transaccionId);
            donacion.setAnonimato(false);
            donacion.setFrecuencia(Donacion.Frecuencia.única);
            donacion.setSuscripcionId(suscripcionExistente.getId());
            donacionRepository.save(donacion);
            
            return ResponseEntity.ok("Pago registrado para suscripción existente");
        }
    }
}