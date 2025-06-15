package com.example.tech.mails;

import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import io.github.cdimascio.dotenv.Dotenv;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

@Service
public class EmailSenderService {

    private final JavaMailSender mailSender;

    private Dotenv dotenv = Dotenv.load();
    private Map<String, String> htmlTemplates;
    
    @Value("${spring.mail.username}")
    String email;
    
    public EmailSenderService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
        this.htmlTemplates = new HashMap<>();

        htmlTemplates.put("feedback", """
    	    <div style='background: #f5f8fa; padding: 30px; font-family: "Segoe UI", sans-serif;'>
    	        <div style='max-width: 600px; margin: auto; background: #ffffff; border-radius: 10px; padding: 30px; box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);'>
    	            <div style='text-align: center; margin-bottom: 20px;'>
    	                <img src='https://tech-yj96.onrender.com/favicon.ico' alt='Tech2Xplore' width='60' height='60' style='border-radius: 50%;' />
    	                <h2 style='margin: 10px 0 0; color: #2c3e50;'>Your Feedback Matters!</h2>
    	            </div>
    	            <p style='font-size: 16px; color: #333;'>We’ve received your message:</p>
    	            <div style='margin: 20px 0; padding: 20px; background-color: #f0f8ff; border-left: 5px solid #3498db; border-radius: 6px;'>
    	                <p style='font-style: italic; color: #555; margin: 0;'>“${content}”</p>
    	            </div>
    	            <p style='font-size: 14px; color: #555;'>Thanks for helping us improve - Tech2Xplore!</p>
    	        </div>
    	    </div>
    	""");
        
        htmlTemplates.put("login", """
    	    <div style='background: #f5f8fa; padding: 30px; font-family: "Segoe UI", sans-serif;'>
    	        <div style='max-width: 600px; margin: auto; background: white; border-radius: 10px; padding: 30px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>
    	            <div style='text-align: center; margin-bottom: 20px;'>
    	                <img src='https://tech-yj96.onrender.com/favicon.ico' alt='Tech2Xplore' width='60' height='60' />
    	                <h2 style='color: #2c3e50;'>Login Alert</h2>
    	            </div>
    	            <p>“Hello <strong>${name}</strong>”,</p>
    	            <p>We noticed a login to your account <strong>@${username}</strong>. If this was you, no action is needed.</p>
    	            <p>If it wasn’t, please reset your password immediately.</p>
    	        </div>
    	    </div>
    	""");
        
        htmlTemplates.put("register", """
    	    <div style='background: #f5f8fa; padding: 30px; font-family: "Segoe UI", sans-serif;'>
    	        <div style='max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 4px 12px rgba(0,0,0,0.1);'>
    	            <div style='text-align: center; margin-bottom: 20px;'>
    	                <img src='https://tech-yj96.onrender.com/favicon.ico' alt='Tech2Xplore' width='60' height='60' />
    	                <h2 style='color: #28a745;'>“Welcome, <strong>${name}</strong>! Login with your username <strong>@${username}</strong>”</h2>
    	            </div>
    	            <p>Thanks for joining Tech2Xplore. We're excited to have you with us.</p>
    	            <p style='color: #666;'>Feel free to reach out if you need any help!</p>
    	        </div>
    	    </div>
    	""");
        
        htmlTemplates.put("change-password", """
    	    <div style='background: #fff8e1; padding: 30px; font-family: "Segoe UI", sans-serif;'>
    	        <div style='max-width: 600px; margin: auto; background: white; padding: 30px; border-radius: 10px; border: 1px solid #ffeeba; box-shadow: 0 4px 12px rgba(0,0,0,0.05);'>
    	            <div style='text-align: center; margin-bottom: 20px;'>
    	                <img src='https://tech-yj96.onrender.com/favicon.ico' alt='Tech2Xplore' width='60' height='60' />
    	                <h2 style='color: #856404;'>Password Changed</h2>
    	            </div>
    	            <p>“Hi <strong>${name}</strong>”,</p>
    	            <p>Your account <strong>@${username}</strong> password was recently changed. If you didn’t do this, please contact support immediately.</p>
    	        </div>
    	    </div>
    	""");

    }

    public void sendEmail(String to, String subject, String templateKey, Map<String, String> placeholders){
       
    	try { 
	    	String template = htmlTemplates.get(templateKey);
	        if (template == null) {
	            throw new IllegalArgumentException("Template not found: " + templateKey);
	        }
	
	        // Replace placeholders like ${username}, ${content}
	        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
	            template = template.replace("${" + entry.getKey() + "}", entry.getValue());
	        }
	
	        MimeMessage message = mailSender.createMimeMessage();
	        MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
	        helper.setTo(to);
	        helper.setSubject(subject);
	        helper.setText(template, true);
	
	        mailSender.send(message);
        
    	}catch(Exception e) {
    		System.out.println("Mail Failed " );
    		e.printStackTrace();
    	}
    }
}
