package org.jucamdonaciones.swgdsjucambackend.controller;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.Map;

import org.jucamdonaciones.swgdsjucambackend.model.PasswordResetToken;
import org.jucamdonaciones.swgdsjucambackend.model.User;
import org.jucamdonaciones.swgdsjucambackend.payload.LoginRequest;
import org.jucamdonaciones.swgdsjucambackend.repository.PasswordResetTokenRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.UserRepository;
import org.jucamdonaciones.swgdsjucambackend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;


@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

    private final RestTemplate restTemplate = new RestTemplate();


    public AuthController(
        AuthenticationManager authenticationManager,
        UserRepository userRepository,
        EmailService emailService,
        PasswordResetTokenRepository passwordResetTokenRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest, HttpServletRequest request) {
         // 1) Validar reCAPTCHA
        String verifyUrl = "https://www.google.com/recaptcha/api/siteverify"
            + "?secret=" + recaptchaSecret
            + "&response=" + loginRequest.getRecaptchaToken();
        @SuppressWarnings("unchecked")
        Map<String,Object> googleResponse = restTemplate.postForObject(verifyUrl, null, Map.class);
        boolean success = (Boolean) googleResponse.get("success");
        if (!success) {
            return ResponseEntity
                .status(400)
                .body("reCAPTCHA inválido");
        }
        
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );

            // Establecer la autenticación en el contexto de seguridad
            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Variable de sesión que indica que el usuario está autenticado
            request.getSession().setAttribute("loggedIn", true);

            // Obtener el usuario autenticado y sus detalles (email y rol)
            User user = userRepository.findByEmail(loginRequest.getEmail());
            // Construir la respuesta en formato JSON
            java.util.Map<String, Object> responseData = new java.util.HashMap<>();
            responseData.put("email", user.getEmail());
            responseData.put("rol", user.getRol().getNombre());
            responseData.put("requiereCambio", user.getRequiere_cambio_contraseña());

            return ResponseEntity.ok()
                .header("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0")
                .body(responseData);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).body("Credenciales incorrectas");
        }
    }


    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestParam String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body("Usuario no encontrado");
        }

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setToken(token);
        resetToken.setUser(user);
        resetToken.setExpiryDate(LocalDateTime.now().plusHours(24));

        passwordResetTokenRepository.save(resetToken);

        String resetLink = "https://jucamdonaciones.org/reset-password?token=" + token;
        // Lógica para enviar el correo electrónico con el enlace
         // Enviar el correo electrónico utilizando el servicio
         emailService.sendResetEmail(email, resetLink);

        return ResponseEntity.ok("Enlace de restablecimiento enviado al correo electrónico");
    }

    @PostMapping("/reset-password")
    @Transactional
    public ResponseEntity<?> resetPassword(@RequestParam String token) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token);
        if (resetToken == null || resetToken.getExpiryDate().isBefore(LocalDateTime.now())) {
            return ResponseEntity.badRequest().body("Token inválido o expirado");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode("Password1"));
        user.setRequiere_cambio_contraseña(true);
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        // Redirigir al usuario o mostrar instrucciones
        return ResponseEntity.ok("Contraseña restablecida. Inicia sesión y cambia tu contraseña.");
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null) {
            new SecurityContextLogoutHandler().logout(request, response, authentication);
        }

        // Invalidar la sesión si existe
        if (request.getSession(false) != null) {
            request.getSession().invalidate();
        }


        return ResponseEntity.ok("Sesión cerrada exitosamente");
    }


    @PostMapping("/reiniciar-password")
    public ResponseEntity<?> reiniciarPassword(@RequestParam String email) {
        User user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.badRequest().body("No se encontró el usuario. Verifica que tu correo sea correcto");
        }
        try {
            // Resetear la contraseña a "Password1" y marcar que se requiere cambio
            user.setPassword(passwordEncoder.encode("Password1"));
            user.setRequiere_cambio_contraseña(true);
            userRepository.save(user);

            // Enviar el correo de forma asíncrona para que no bloquee la respuesta
            new Thread(() -> {
                try {
                    String resetLink = "https://jucamdonaciones.org/contrasena";
                    emailService.sendResetEmail(email, resetLink);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }).start();

            return ResponseEntity.ok("Se envió un correo con las instrucciones para reestablecer tu contraseña");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("No se pudo reestablecer tu contraseña. Intenta de nuevo más tarde");
        }
    }


}
