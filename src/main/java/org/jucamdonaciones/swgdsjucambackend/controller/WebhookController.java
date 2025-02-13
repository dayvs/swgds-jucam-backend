package org.jucamdonaciones.swgdsjucambackend.controller;

import org.jucamdonaciones.swgdsjucambackend.payload.WebhookPayload;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    // Guarda la cabecera de autorización de forma centralizada
    private static final String AUTH_HEADER = "Basic NTYxZTY4ODgtNDJlNi00OTNhLTg3ODQtZDgyNDVlMTE3YTlkOmFmMzljYWYyLTgyMGYtNDAzMC1hYWU5LTlkNGU2ZDE2ZmI0YQ==";

    @PostMapping("/pagos")
    public ResponseEntity<?> recibirWebhook(@RequestBody WebhookPayload payload) {
        // Registrar el payload recibido (útil para debug)
        System.out.println("Webhook recibido: " + payload);

        // Extraer el payment_request_id y event_type del payload
        String paymentRequestId = payload.getId();
        String eventType = payload.getEvent_type();

        // (Opcional) Aquí se podría validar la firma del webhook, si la API de Clip lo requiere.

        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", AUTH_HEADER);

        // ------------------------------------------------------------
        // Paso 1: Consultar el estado de la solicitud de pago (checkout)
        // ------------------------------------------------------------
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
            // Extraer receipt_no ya que solo se encuentra cuando el status es COMPLETED
            String receiptNo = (String) checkoutResponse.get("receipt_no");
            if (receiptNo == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("No se encontró receipt_no en la respuesta, a pesar de ser CHECKOUT_COMPLETED");
            }
            
            // ------------------------------------------------------------
            // Paso 2: Consultar la información del pago (payment)
            // ------------------------------------------------------------
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

            // Combinamos la información obtenida para guardarla en la BD.
            return ResponseEntity.ok("Webhook procesado correctamente con status COMPLETED");
        } else {
            // Para otros status (por ejemplo, CHECKOUT_CREATED o CHECKOUT_PENDING)
            System.out.println("Status es " + status + ". Se omite la consulta a payments.");
            return ResponseEntity.ok("Webhook procesado correctamente. Status: " + status + " (sin consulta a payments)");
        }
    }
}