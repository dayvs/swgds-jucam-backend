package org.jucamdonaciones.swgdsjucambackend.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.jucamdonaciones.swgdsjucambackend.model.PasswordResetToken;
import org.jucamdonaciones.swgdsjucambackend.model.User;
import org.jucamdonaciones.swgdsjucambackend.payload.LoginRequest;
import org.jucamdonaciones.swgdsjucambackend.repository.PasswordResetTokenRepository;
import org.jucamdonaciones.swgdsjucambackend.repository.UserRepository;
import org.jucamdonaciones.swgdsjucambackend.service.EmailService;
//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;



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
    public ResponseEntity<?> authenticateUser(@RequestBody LoginRequest loginRequest) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    loginRequest.getEmail(),
                    loginRequest.getPassword()
                )
            );
            //validar nombre
            String username = authentication.getName();
            return ResponseEntity.ok("Usuario " + username + " autenticado exitosamente");
            
            // Lógica adicional si es necesario    
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
}
