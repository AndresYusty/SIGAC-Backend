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
        String body = layout("Session notice",
                "The agenda has been closed and you are summoned to the session of the <strong>" + esc(councilType)
                        + "</strong> of the " + esc(faculty) + ".",
                "<strong>Date:</strong> " + esc(date) + "<br/><strong>Time:</strong> " + esc(time)
                        + "<br/><strong>Location:</strong> " + esc(location),
                "The official notice and the supporting documents for each item are attached.");
        send(recipients, "Session notice - " + councilType, body, attachments);
    }

    @Async
    public void sendRequestReturned(String recipient, String code, String title, String reason, boolean rejected) {
        String action = rejected ? "rejected" : "returned for adjustments";
        String body = layout("Request " + action,
                "Your request <strong>" + esc(code) + "</strong> (\"" + esc(title) + "\") was " + action + ".",
                "<strong>Reason:</strong> " + esc(reason),
                rejected ? "" : "You can correct it and resubmit it from SIGAC.");
        send(List.of(recipient), "Request " + code + " " + action, body, List.of());
    }

    @Async
    public void sendMinutesPublished(List<String> recipients, String serialNumber, String faculty, String pdfPath) {
        String body = layout("Minutes published: " + serialNumber,
                "The official minutes <strong>" + esc(serialNumber) + "</strong> of the " + esc(faculty)
                        + " have been signed and published.",
                "The PDF/A document is attached. You can also view it in the SIGAC Digital Repository "
                        + "(<a href=\"" + esc(props.getFrontendUrl()) + "/repository\">open</a>).", "");
        send(recipients, "Minutes published " + serialNumber, body, List.of(pdfPath));
    }

    @Async
    public void sendPasswordReset(String recipient, String name, String token) {
        String link = props.getFrontendUrl() + "/reset-password?token=" + token;
        String body = layout("Reset password",
                "Hello " + esc(name) + ", we received a request to reset your SIGAC password.",
                "Use this single-use link: <a href=\"" + esc(link) + "\">reset password</a>.",
                "If you did not request it, please ignore this message.");
        send(List.of(recipient), "SIGAC - Reset password", body, List.of());
    }

    @Async
    public void sendIntegrityAlert(List<String> recipients, String serialNumber) {
        String body = layout("ALERT: compromised document",
                "The minutes <strong>" + esc(serialNumber) + "</strong> failed the SHA-256 integrity check.",
                "The system blocked its download. Check the storage volume and the audit log.", "");
        send(recipients, "SIGAC ALERT - Integrity compromised " + serialNumber, body, List.of());
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
        return sb.append("<hr/><p style=\"font-size:12px;color:#777\">Automatic message from SIGAC.</p></div>").toString();
    }

    private String esc(String s) {
        return HtmlSanitizer.escapeXml(s);
    }
}
