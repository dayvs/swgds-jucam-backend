package org.jucamdonaciones.swgdsjucambackend.controller;

import java.util.HashMap;
import java.util.Map;

import org.jucamdonaciones.swgdsjucambackend.payload.PaymentRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/pago")
public class PagoController {

    @PostMapping
    public ResponseEntity<?> crearPago(@RequestBody PaymentRequest paymentRequest) {
        try {
            // Construir el payload para la API de Checkout de Clip
            Map<String, Object> payload = new HashMap<>();
            payload.put("amount", paymentRequest.getAmount());
            payload.put("currency", "MXN");
            
            // Concatenar los datos del formulario para purchase_description
            String purchaseDescription = paymentRequest.getName() + "-" + paymentRequest.getEmail() + "-" + paymentRequest.getPhone();
            payload.put("purchase_description", purchaseDescription);
            
            // Datos del cliente
            Map<String, Object> customerInfo = new HashMap<>();
            customerInfo.put("name", paymentRequest.getName());
            customerInfo.put("email", paymentRequest.getEmail());
            try {
                // Se intenta convertir el teléfono a número
                Long phoneNumber = Long.parseLong(paymentRequest.getPhone());
                customerInfo.put("phone", phoneNumber);
            } catch (NumberFormatException e) {
                // Si no se puede convertir, se envía tal cual
                customerInfo.put("phone", paymentRequest.getPhone());
            }
            payload.put("customer_info", customerInfo);
            
            // URLs de redirección según el resultado del pago
            Map<String, String> redirectionUrl = new HashMap<>();
            redirectionUrl.put("success", "https://jucamdonaciones.org/donacionexito");
            redirectionUrl.put("error", "https://jucamdonaciones.org/donacionerror");
            redirectionUrl.put("default", "https://jucamdonaciones.org/donar");
            payload.put("redirection_url", redirectionUrl);
            
            // URL del webhook
            payload.put("webhook_url", "https://swgds-jucam-backend.onrender.com/webhook/pagos");
            
            
            // Instanciar RestTemplate (incluido en spring-boot-starter-web)
            RestTemplate restTemplate = new RestTemplate();
            
            // Configurar los headers de la petición
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Basic NTYxZTY4ODgtNDJlNi00OTNhLTg3ODQtZDgyNDVlMTE3YTlkOmFmMzljYWYyLTgyMGYtNDAzMC1hYWU5LTlkNGU2ZDE2ZmI0YQ==");
            
            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(payload, headers);
            
            // URL de la API de Checkout de Clip
            String clipApiUrl = "https://api.payclip.com/v2/checkout";
            
            // Llamada POST a la API de Clip
            ResponseEntity<Map> response = restTemplate.postForEntity(clipApiUrl, requestEntity, Map.class);
            
            if(response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
                // Se espera que la respuesta incluya el campo "payment_request_url"
                Map<String, Object> responseBody = response.getBody();
                String paymentUrl = (String) responseBody.get("payment_request_url");
                
                // Se retorna la URL de pago al front-end
                Map<String, String> result = new HashMap<>();
                result.put("paymentUrl", paymentUrl);
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.status(response.getStatusCode()).body("Error al crear el pago");
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error interno: " + e.getMessage());
        }
    }
}