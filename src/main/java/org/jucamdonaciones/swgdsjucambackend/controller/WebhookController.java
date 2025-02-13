package org.jucamdonaciones.swgdsjucambackend.controller;

import org.jucamdonaciones.swgdsjucambackend.payload.WebhookPayload;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook")
public class WebhookController {

    @PostMapping("/pagos")
    public ResponseEntity<?> recibirWebhook(@RequestBody WebhookPayload payload) {        
        // Se registra el payload recibido para confirmar la recepción.
        System.out.println("Webhook recibido: " + payload);
        
        String paymentRequestId = payload.getId();
        String eventType = payload.getEvent_type();

        // Aquí se podría agregar la lógica para procesar el webhook:
        // - Si event_type es "INSERT", es un nuevo pago.
        // - Si es "UPDATE", se actualiza el status del pago.
        System.out.println("Payment Request ID: " + paymentRequestId);
        System.out.println("Event Type: " + eventType);

        // Retornamos una respuesta exitosa.
        return ResponseEntity.ok("Webhook recibido correctamente");
    }
}