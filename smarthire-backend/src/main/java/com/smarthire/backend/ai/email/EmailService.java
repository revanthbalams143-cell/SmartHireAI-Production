package com.smarthire.backend.ai.email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Properties;

@Service
public class EmailService {
    private final String smtpHost; private final int smtpPort; private final String smtpUsername; private final String smtpPassword; private final boolean smtpAuth; private final boolean smtpStartTls; private final String fromAddress;
    public EmailService(@Value("${mail.smtp.host:}") String smtpHost,@Value("${mail.smtp.port:587}") int smtpPort,@Value("${mail.smtp.username:}") String smtpUsername,@Value("${mail.smtp.password:}") String smtpPassword,@Value("${mail.smtp.auth:false}") boolean smtpAuth,@Value("${mail.smtp.starttls.enable:true}") boolean smtpStartTls,@Value("${mail.from:}") String fromAddress){this.smtpHost=smtpHost;this.smtpPort=smtpPort;this.smtpUsername=smtpUsername;this.smtpPassword=smtpPassword;this.smtpAuth=smtpAuth;this.smtpStartTls=smtpStartTls;this.fromAddress=fromAddress;}
    public boolean isConfigured(){return smtpHost!=null&&!smtpHost.isBlank()&&fromAddress!=null&&!fromAddress.isBlank();}
    private Session session(){Properties props=new Properties();props.put("mail.smtp.host",smtpHost);props.put("mail.smtp.port",String.valueOf(smtpPort));props.put("mail.smtp.auth",String.valueOf(smtpAuth));props.put("mail.smtp.starttls.enable",String.valueOf(smtpStartTls));return Session.getInstance(props,new jakarta.mail.Authenticator(){protected jakarta.mail.PasswordAuthentication getPasswordAuthentication(){return new jakarta.mail.PasswordAuthentication(smtpUsername,smtpPassword);}});}
    public void sendEmail(String to,String subject,String body)throws MessagingException, IOException{if(!isConfigured())throw new MessagingException("SMTP is not configured. Set mail.smtp.host and mail.from to enable email delivery.");MimeMessage message=new MimeMessage(session());message.setFrom(new InternetAddress(fromAddress));message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to));message.setSubject(subject);message.setText(body);Transport.send(message);}
    public void sendEmailWithAttachment(String to,String subject,String body,byte[] attachment,String fileName)throws MessagingException, IOException{if(!isConfigured())throw new MessagingException("SMTP is not configured. Set mail.smtp.host and mail.from to enable email delivery.");MimeMessage message=new MimeMessage(session());message.setFrom(new InternetAddress(fromAddress));message.setRecipients(Message.RecipientType.TO,InternetAddress.parse(to));message.setSubject(subject);MimeBodyPart text=new MimeBodyPart();text.setText(body);MimeBodyPart file=new MimeBodyPart();file.setDataHandler(new jakarta.activation.DataHandler(new jakarta.mail.util.ByteArrayDataSource(new ByteArrayInputStream(attachment),"application/pdf")));file.setFileName(fileName);MimeMultipart multipart=new MimeMultipart();multipart.addBodyPart(text);multipart.addBodyPart(file);message.setContent(multipart);Transport.send(message);}
}
