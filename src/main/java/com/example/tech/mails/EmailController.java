package com.example.tech.mails;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EmailController {

    private final EmailSenderService emailSenderService;

    public EmailController(EmailSenderService emailSenderService) {
        this.emailSenderService = emailSenderService;
    }

//    @GetMapping("/login/send-mail")
    public String sendEmail(@RequestParam String to, @RequestParam String subject, @RequestParam String content) {
        try {
            emailSenderService.sendEmail(to, subject, content);
            return "Email sent successfully to " + to;
        } catch (Exception e) {
            return "Failed to send email: " + e.getMessage();
        }
    }
}
