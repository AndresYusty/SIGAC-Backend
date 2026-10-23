package com.university.sigac.modules.notifications.service;

import com.university.sigac.config.AppProperties;
import com.university.sigac.core.storage.StorageService;
import com.university.sigac.core.util.HtmlSanitizer;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
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

    private static final Locale ES = Locale.forLanguageTag("es");
    private static final byte[] LOGO = loadLogo();

    private final JavaMailSender mailSender;
    private final AppProperties props;
    private final StorageService storage;

    @Async
    public void sendNotice(List<String> recipients, String councilType, String faculty, String date,
                           String time, String location, List<String> attachments) {
        String body = layout("Citación a sesión", null,
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
        String body = layout("Solicitud " + action, null,
                "Su solicitud <strong>" + esc(code) + "</strong> (\"" + esc(title) + "\") fue " + action + ".",
                "<strong>Motivo:</strong> " + esc(reason),
                rejected ? "" : "Puede corregirla y volver a radicarla en SIGAC.");
        send(List.of(recipient), "Solicitud " + code + " " + action, body, List.of());
    }

    @Async
    public void sendMinutesPublished(List<String> recipients, String serialNumber, String faculty, String pdfPath) {
        String body = layout("Acta publicada: " + serialNumber, null,
                "El acta <strong>" + esc(serialNumber) + "</strong> de la " + esc(faculty)
                        + " fue firmada y publicada.",
                "Se adjunta el PDF. También puede verla en el repositorio de SIGAC "
                        + "(<a href=\"" + esc(props.getFrontendUrl()) + "/repository\">abrir</a>).", "");
        send(recipients, "Acta publicada " + serialNumber, body, List.of(pdfPath));
    }

    @Async
    public void sendPasswordReset(String recipient, String name, String token) {
        String link = props.getFrontendUrl() + "/reset-password?token=" + token;
        String body = layout("Restablecer contraseña", name,
                "Recibimos una solicitud para restablecer su contraseña de SIGAC.",
                "Use este enlace de un solo uso: <a href=\"" + esc(link) + "\">restablecer contraseña</a>.",
                "Si usted no lo solicitó, ignore este mensaje.");
        send(List.of(recipient), "SIGAC - Restablecer contraseña", body, List.of());
    }

    @Async
    public void sendIntegrityAlert(List<String> recipients, String serialNumber) {
        String body = layout("Alerta: documento alterado", null,
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
                if (LOGO.length > 0) {
                    h.addInline("usco-logo", new ByteArrayResource(LOGO), "image/png");
                }
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
        String name = p.getFileName().toString().replaceFirst("^[0-9a-fA-F-]{36}-", "");
        if (name.equalsIgnoreCase("citacion.pdf")) {
            return "Citación.pdf";
        }
        return name;
    }

    /**
     * Institutional sheet: USCO mark, university name, message box and the confidentiality note.
     * {@code recipientName} is optional; the greeting stays "Estimado(a):" when it is absent.
     */
    private String layout(String title, String recipientName, String p1, String p2, String p3) {
        String greeting = recipientName == null || recipientName.isBlank()
                ? "Estimado(a):"
                : "Estimado(a): " + esc(recipientName.toUpperCase(ES));
        StringBuilder body = new StringBuilder();
        body.append("<p style=\"margin:0 0 16px;font-weight:700\">").append(greeting).append("</p>");
        for (String p : List.of(p1, p2, p3)) {
            if (p != null && !p.isBlank()) {
                body.append("<p style=\"margin:0 0 16px\">").append(p).append("</p>");
            }
        }
        body.append("<p style=\"margin:8px 0 0\">Correo generado automáticamente, favor no responder.</p>");
        return """
                <body style="margin:0;padding:0;background:#f2f2f2;">
                <table role="presentation" width="100%%" cellpadding="0" cellspacing="0" style="background:#f2f2f2;">
                <tr><td align="center" style="padding:28px 16px;">
                <table role="presentation" width="640" cellpadding="0" cellspacing="0" style="width:640px;max-width:640px;">
                <tr><td align="center" style="padding-bottom:14px;">
                  <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                    <td style="vertical-align:middle;"><img src="cid:usco-logo" width="78" alt="Universidad Surcolombiana" style="display:block;border:0;"/></td>
                    <td style="vertical-align:middle;padding-left:12px;font-family:Arial,Helvetica,sans-serif;font-weight:700;font-size:20px;line-height:1.05;letter-spacing:0.4px;color:#8f141b;">UNIVERSIDAD<br/>SURCOLOMBIANA</td>
                  </tr></table>
                </td></tr>
                <tr><td align="center" style="font-family:Arial,Helvetica,sans-serif;font-size:22px;font-weight:700;color:#222;padding-bottom:4px;">Universidad Surcolombiana</td></tr>
                <tr><td align="center" style="font-family:Arial,Helvetica,sans-serif;font-size:16px;font-weight:700;color:#222;padding-bottom:18px;">%s</td></tr>
                <tr><td style="background:#f7f7f7;border:1px solid #e3e3e3;padding:22px 26px;font-family:Arial,Helvetica,sans-serif;font-size:15px;line-height:1.45;color:#222;">%s</td></tr>
                <tr><td style="padding-top:16px;font-family:Arial,Helvetica,sans-serif;font-size:11px;line-height:1.4;color:#444;">NOTA CONFIDENCIAL: Este mensaje y sus anexos son de uso institucional de la Universidad Surcolombiana, enviados por SIGAC. Si usted no es el destinatario, notifíquelo y elimínelo. El tratamiento de los datos personales se ajusta a la Ley 1581 de 2012.</td></tr>
                </table>
                </td></tr>
                </table>
                </body>
                """.formatted(esc(title), body);
    }

    private static byte[] loadLogo() {
        try (InputStream in = EmailService.class.getResourceAsStream("/sigac/brand/usco-logo.png")) {
            return in == null ? new byte[0] : in.readAllBytes();
        } catch (IOException e) {
            return new byte[0];
        }
    }

    private String esc(String s) {
        return HtmlSanitizer.escapeXml(s);
    }
}
