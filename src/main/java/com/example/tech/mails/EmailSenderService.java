package com.example.tech.mails;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import io.github.cdimascio.dotenv.Dotenv;

@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    private Dotenv dotenv = Dotenv.load();
    
    @Value("${spring.mail.username}")
    String email;
    
    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendEmail(String to, String subject, String content) {
        System.out.println("Email Is Sending");
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(email);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(content);

            mailSender.send(message);
            System.out.println("Email Sent Successfully");

        } catch (Exception e) {
            System.err.println("Error while sending email: " + e.getMessage());
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
