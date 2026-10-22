package com.university.sigac.modules.notifications.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.storage.StorageService;
import com.university.sigac.core.util.HtmlSanitizer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Email notifications. Every method is @Async: it runs on another thread and does not delay the response to the user.
 * The SMTP server is configured in application.yml (spring.mail.*). If one recipient fails, the others are still processed.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties props;
    private final StorageService storage;

    @Async
    public void sendNotice(List<String> recipients, String councilType, String faculty, String date,
                           String time, String location, List<String> attachments) {
        String body = layout("Citación a sesión",
                "Se cerró el orden del día y queda convocado a la sesión del <strong>" + esc(councilType)
                        + "</strong> de la " + esc(faculty) + ".",
                "<strong>Fecha:</strong> " + esc(date) + "<br/><strong>Hora:</strong> " + esc(time)
                        + "<br/><strong>Lugar:</strong> " + esc(location),
                "Se adjuntan la citación y los soportes de cada punto.");
        send(recipients, "Citación a sesión - " + councilType, body, attachments);
    }

    @Async
    public void sendRequestReturned(String recipient, String code, String title, String reason, boolean rejected) {
        String action = rejected ? "rechazada" : "devuelta para ajustes";
        String body = layout("Solicitud " + action,
                "Su solicitud <strong>" + esc(code) + "</strong> (\"" + esc(title) + "\") fue " + action + ".",
                "<strong>Motivo:</strong> " + esc(reason),
                rejected ? "" : "Puede corregirla y volver a radicarla en SIGAC.");
        send(List.of(recipient), "Solicitud " + code + " " + action, body, List.of());
    }

    @Async
    public void sendMinutesPublished(List<String> recipients, String serialNumber, String faculty, String pdfPath) {
        String body = layout("Acta publicada: " + serialNumber,
                "El acta <strong>" + esc(serialNumber) + "</strong> de la " + esc(faculty)
                        + " fue firmada y publicada.",
                "Se adjunta el PDF. También puede verla en el repositorio de SIGAC "
                        + "(<a href=\"" + esc(props.getFrontendUrl()) + "/repository\">abrir</a>).", "");
        send(recipients, "Acta publicada " + serialNumber, body, List.of(pdfPath));
    }

    @Async
    public void sendPasswordReset(String recipient, String name, String token) {
        String link = props.getFrontendUrl() + "/reset-password?token=" + token;
        String body = layout("Restablecer contraseña",
                "Hola " + esc(name) + ", recibimos una solicitud para restablecer su contraseña de SIGAC.",
                "Use este enlace de un solo uso: <a href=\"" + esc(link) + "\">restablecer contraseña</a>.",
                "Si usted no lo solicitó, ignore este mensaje.");
        send(List.of(recipient), "SIGAC - Restablecer contraseña", body, List.of());
    }

    @Async
    public void sendIntegrityAlert(List<String> recipients, String serialNumber) {
        String body = layout("Alerta: documento alterado",
                "El acta <strong>" + esc(serialNumber) + "</strong> no superó la verificación de integridad.",
                "El sistema bloqueó su descarga. Revise el almacenamiento y la auditoría.", "");
        send(recipients, "SIGAC - Acta alterada " + serialNumber, body, List.of());
    }

    // ------------------------------------------------------------------

    private void send(List<String> recipients, String subject, String html, List<String> attachments) {
        if (recipients == null || recipients.isEmpty()) {
            return;
        }
        if (!props.getMail().isEnabled()) {
            log.info("[email disabled] '{}' not sent to {} recipient(s). Enable sigac.mail.enabled.", subject,
                    recipients.size());
            return;
        }
        for (String to : recipients) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
                h.setFrom(props.getMail().getFrom());
                h.setTo(to);
                h.setSubject(subject);
                h.setText(html, true);
                for (String rel : attachments) {
                    Path p = storage.resolve(rel);
                    if (Files.exists(p)) {
                        h.addAttachment(displayName(p), new FileSystemResource(p));
                    }
                }
                mailSender.send(msg);
            } catch (MessagingException | RuntimeException e) {
                log.error("Could not send '{}' to {}: {}", subject, to, e.getMessage());
            }
        }
    }

    /** Attachments are stored as "<uuid>-<name>.pdf": only the original name is shown. */
    private String displayName(Path p) {
        return p.getFileName().toString().replaceFirst("^[0-9a-fA-F-]{36}-", "");
    }

    private String layout(String title, String p1, String p2, String p3) {
        StringBuilder sb = new StringBuilder("<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#222\">");
        sb.append("<h2 style=\"color:#1a3c6e\">").append(esc(title)).append("</h2>");
        for (String p : List.of(p1, p2, p3)) {
            if (!p.isBlank()) {
                sb.append("<p>").append(p).append("</p>");
            }
        }
        return sb.append("<hr/><p style=\"font-size:12px;color:#777\">Mensaje automático de SIGAC. No responda a este correo.</p></div>").toString();
    }

    private String esc(String s) {
        return HtmlSanitizer.escapeXml(s);
    }
}
