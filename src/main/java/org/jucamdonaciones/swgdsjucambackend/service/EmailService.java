package org.jucamdonaciones.swgdsjucambackend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reestablecer contraseña JUCAM");
        String body = "Para reestablecer tu contraseña ingresa al siguiente link, proporciona tu usuario y la contraseña por default. Posteriormente crea una nueva contraseña.\n\n";
        body += resetLink + "\n\n";
        body += "Reestablecer contraseña";
        message.setText(body);
        mailSender.send(message);
    }
}