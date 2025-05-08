package org.yakdanol.notificationservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
@ConfigurationProperties(prefix = "emailconfig")
@Getter
@Setter
public class MailConfig {

    private boolean emailEnabled;
    private String smtpHost;
    private int smtpPort;
    private String smtpUsername;
    private String smtpPassword;
    private int smtpConnectionPoolSize;

    @Bean
    public JavaMailSender getJavaMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(smtpHost);
        mailSender.setPort(smtpPort);
        mailSender.setUsername(smtpUsername);
        mailSender.setPassword(smtpPassword);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        props.put("mail.smtp.pool", "true");
        props.put("mail.smtp.maxconnections", smtpConnectionPoolSize);
        props.put("mail.smtp.socketFactory.port", smtpPort);

        return mailSender;
    }
}
