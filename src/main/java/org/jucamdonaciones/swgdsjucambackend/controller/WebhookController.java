package org.jucamdonaciones.swgdsjucambackend.controller;

import java.math.BigDecimal;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.jucamdonaciones.swgdsjucambackend.model.Donacion;
import org.jucamdonaciones.swgdsjucambackend.model.Donador;
import org.jucamdonaciones.swgdsjucambackend.payload.WebhookPayload;
import org.jucamdonaciones.swgdsjucambackend.repository.DonacionRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.DonadorRepository;
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
public class WebhookController {

    // Guarda la cabecera de autorización de forma centralizada
    private static final String AUTH_HEADER = "Basic NTYxZTY4ODgtNDJlNi00OTNhLTg3ODQtZDgyNDVlMTE3YTlkOmFmMzljYWYyLTgyMGYtNDAzMC1hYWU5LTlkNGU2ZDE2ZmI0YQ==";

    @Autowired
    private DonadorRepository donadorRepository;

    @Autowired
    private DonacionRepository donacionRepository;


    @PostMapping("/pagos")
    public ResponseEntity<?> recibirWebhook(@RequestBody WebhookPayload payload) {
        // Registrar el payload recibido (útil para debug)
        System.out.println("Webhook recibido: " + payload);

        // Extraer el payment_request_id y event_type del payload
        String paymentRequestId = payload.getId();
        String eventType = payload.getEvent_type();

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_HEADER);
        
        // Paso 1: Consultar el estado de la solicitud de pago (checkout)        
        String checkoutUrl = "https://api.payclip.com/v2/checkout/" + paymentRequestId;
        HttpEntity<String> checkoutEntity = new HttpEntity<>(headers);

        ResponseEntity<Map> checkoutResponseEntity;
        try {
            checkoutResponseEntity = restTemplate.exchange(checkoutUrl, HttpMethod.GET, checkoutEntity, Map.class);
        } catch (Exception e) {
            System.err.println("Error al consultar checkout: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al consultar checkout");
        }

        Map<String, Object> checkoutResponse = checkoutResponseEntity.getBody();
        if (checkoutResponse == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Respuesta vacía del checkout");
        }
        System.out.println("Respuesta de checkout: " + checkoutResponse);

        // Validar el status de la solicitud de pago
        String status = (String) checkoutResponse.get("status");
        if (status == null) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se encontró el status en la respuesta");
        }

        // Solo proceder al paso 2 si el status es CHECKOUT_COMPLETED
        if ("CHECKOUT_COMPLETED".equals(status)) {
            // Extraer receipt_no
            String receiptNo = (String) checkoutResponse.get("receipt_no");
            if (receiptNo == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se encontró receipt_no en la respuesta, a pesar de ser CHECKOUT_COMPLETED");
            }
            
            
            // Paso 2: Consultar la información del pago (payment)
            
            // Calcular "from" y "to" en formato ISO 8601 para la consulta:
            // "from": día en curso a las 00:00:00 UTC y "to": "from" + 24 horas
            ZonedDateTime nowUtc = ZonedDateTime.now(ZoneOffset.UTC);
            ZonedDateTime from = nowUtc.toLocalDate().atStartOfDay(ZoneOffset.UTC);
            ZonedDateTime to = from.plusDays(1);
            String fromStr = from.format(DateTimeFormatter.ISO_INSTANT);
            String toStr = to.format(DateTimeFormatter.ISO_INSTANT);

            // Construir la URL de la consulta a payments
            String paymentsUrl = "https://api.payclip.com/payments?from=" + fromStr + "&to=" + toStr + "&receipt_no=" + receiptNo;
            HttpEntity<String> paymentsEntity = new HttpEntity<>(headers);

            ResponseEntity<Map> paymentsResponseEntity;
            try {
                paymentsResponseEntity = restTemplate.exchange(paymentsUrl, HttpMethod.GET, paymentsEntity, Map.class);
            } catch (Exception e) {
                System.err.println("Error al consultar payments: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error al consultar payments");
            }

            Map<String, Object> paymentsResponse = paymentsResponseEntity.getBody();
            if (paymentsResponse == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Respuesta vacía de payments");
            }
            System.out.println("Respuesta de payments: " + paymentsResponse);

            // Extraer información del primer elemento del arreglo "results"
            List<Map<String, Object>> results = (List<Map<String, Object>>) paymentsResponse.get("results");
            if (results == null || results.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("No se encontró información de pagos");
            }
            Map<String, Object> paymentDetails = results.get(0);

            // Extraer el campo "type" del objeto "payment_method"
            Map<String, Object> paymentMethod = (Map<String, Object>) paymentDetails.get("payment_method");
            String paymentMethodType = paymentMethod != null ? (String) paymentMethod.get("type") : null;

            // Extraer los campos "code" y "message" del objeto "status_detail"
            Map<String, Object> statusDetail = (Map<String, Object>) paymentDetails.get("status_detail");
            String statusCode = statusDetail != null ? (String) statusDetail.get("code") : null;
            String statusMessage = statusDetail != null ? (String) statusDetail.get("message") : null;

            System.out.println("Payment Method Type: " + paymentMethodType);
            System.out.println("Status Detail Code: " + statusCode);
            System.out.println("Status Detail Message: " + statusMessage);

            // Determinar el estado de la transacción
            Donacion.Estado estadoDonacion = ("AP-PAI01".equals(statusCode) && "paid".equalsIgnoreCase(statusMessage))
                    ? Donacion.Estado.exitoso
                    : Donacion.Estado.fallido;

            // Registrar la información en la base de datos
            // ----------------------------------------------------------------
            // Extraer y separar purchase_description
            String purchaseDescription = (String) checkoutResponse.get("purchase_description");
            if (purchaseDescription != null) {
                String[] parts = purchaseDescription.split("-");
                if (parts.length == 3) {
                    String donorName = parts[0];
                    String donorEmail = parts[1];
                    String donorTelefono = parts[2];

                    // Registrar en donadores
                    Donador donador = new Donador();
                    donador.setNombre(donorName);
                    donador.setEmail(donorEmail);
                    donador.setTelefono(donorTelefono);
                    // consentimiento_datos ya se inicializa en true y fecha_registro en now
                    donador = donadorRepository.save(donador);

                    // Registrar en donaciones
                    Donacion donacion = new Donacion();
                    donacion.setDonador(donador);
                    
                    // Convertir amount a Double
                    BigDecimal amount;
                    try {
                        amount = new BigDecimal(checkoutResponse.get("amount").toString());
                    } catch (Exception ex) {
                        amount = BigDecimal.ZERO;
                    }
                    donacion.setMonto(amount);
                    donacion.setMetodoPago(paymentMethodType);
                    donacion.setEstado(estadoDonacion);
                    donacion.setTransaccionId(paymentRequestId);
                    donacion.setAnonimato(false);
                    // frecuencia se deja por defecto como UNICA y suscripcion_id en null
                    donacionRepository.save(donacion);
                } else {
                    System.err.println("El purchase_description no tiene el formato esperado: " + purchaseDescription);
                }
            } else {
                System.err.println("No se encontró purchase_description en la respuesta del checkout");
            }

            return ResponseEntity.ok("Webhook procesado y datos guardados correctamente");
        } else {
            System.out.println("Status es " + status + ". No se registra información en BD.");
            return ResponseEntity.ok("Webhook procesado. Status: " + status + " (sin registro en BD)");
        }
    }
}