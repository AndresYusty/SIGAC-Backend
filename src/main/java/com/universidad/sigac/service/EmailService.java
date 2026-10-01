package com.universidad.sigac.service;

import com.universidad.sigac.config.AppProperties;
import com.universidad.sigac.util.HtmlSanitizer;
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
 * Notificaciones por correo. Cada método es @Async: se ejecuta en otro hilo y no demora la respuesta al usuario.
 * El servidor SMTP se configura en application.yml (spring.mail.*). Si un destinatario falla, se sigue con los demás.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final AppProperties props;
    private final StorageService storage;

    @Async
    public void enviarCitacion(List<String> destinatarios, String tipoConsejo, String facultad, String fecha,
                               String hora, String lugar, List<String> adjuntos) {
        String cuerpo = plantilla("Citación a sesión",
                "Se ha cerrado el Orden del Día y se convoca a la sesión del <strong>" + esc(tipoConsejo) + "</strong> de la "
                        + esc(facultad) + ".",
                "<strong>Fecha:</strong> " + esc(fecha) + "<br/><strong>Hora:</strong> " + esc(hora)
                        + "<br/><strong>Lugar:</strong> " + esc(lugar),
                "Adjuntamos la citación oficial y los documentos de soporte de cada punto.");
        enviar(destinatarios, "Citación a sesión - " + tipoConsejo, cuerpo, adjuntos);
    }

    @Async
    public void enviarSolicitudDevuelta(String destinatario, String codigo, String titulo, String motivo, boolean rechazada) {
        String accion = rechazada ? "rechazada" : "devuelta para ajustes";
        String cuerpo = plantilla("Solicitud " + accion,
                "Su solicitud <strong>" + esc(codigo) + "</strong> («" + esc(titulo) + "») fue " + accion + ".",
                "<strong>Motivo:</strong> " + esc(motivo),
                rechazada ? "" : "Puede corregirla y reenviarla desde SIGAC.");
        enviar(List.of(destinatario), "Solicitud " + codigo + " " + accion, cuerpo, List.of());
    }

    @Async
    public void enviarActaPublicada(List<String> destinatarios, String consecutivo, String facultad, String rutaPdf) {
        String cuerpo = plantilla("Acta publicada: " + consecutivo,
                "Se firmó y publicó el acta oficial <strong>" + esc(consecutivo) + "</strong> de la " + esc(facultad) + ".",
                "Se adjunta el documento PDF/A. También puede consultarla en el Repositorio Digital de SIGAC "
                        + "(<a href=\"" + esc(props.getFrontendUrl()) + "/repositorio\">abrir</a>).", "");
        enviar(destinatarios, "Acta publicada " + consecutivo, cuerpo, List.of(rutaPdf));
    }

    @Async
    public void enviarRecuperacionClave(String destinatario, String nombre, String token) {
        String enlace = props.getFrontendUrl() + "/auth/reset-password?token=" + token;
        String cuerpo = plantilla("Restablecer contraseña",
                "Hola " + esc(nombre) + ", recibimos una solicitud para restablecer su contraseña de SIGAC.",
                "Use este enlace de un solo uso: <a href=\"" + esc(enlace) + "\">restablecer contraseña</a>.",
                "Si usted no lo solicitó, ignore este mensaje.");
        enviar(List.of(destinatario), "SIGAC - Restablecer contraseña", cuerpo, List.of());
    }

    @Async
    public void enviarAlertaIntegridad(List<String> destinatarios, String consecutivo) {
        String cuerpo = plantilla("ALERTA: documento comprometido",
                "El acta <strong>" + esc(consecutivo) + "</strong> falló la verificación de integridad SHA-256.",
                "El sistema bloqueó su descarga. Revise el volumen de almacenamiento y la bitácora de auditoría.", "");
        enviar(destinatarios, "ALERTA SIGAC - Integridad comprometida " + consecutivo, cuerpo, List.of());
    }

    // ------------------------------------------------------------------

    private void enviar(List<String> destinatarios, String asunto, String html, List<String> adjuntos) {
        if (destinatarios == null || destinatarios.isEmpty()) {
            return;
        }
        if (!props.getMail().isEnabled()) {
            log.info("[correo deshabilitado] '{}' no enviado a {} destinatario(s). Active sigac.mail.enabled.", asunto,
                    destinatarios.size());
            return;
        }
        for (String destino : destinatarios) {
            try {
                MimeMessage msg = mailSender.createMimeMessage();
                MimeMessageHelper h = new MimeMessageHelper(msg, true, "UTF-8");
                h.setFrom(props.getMail().getFrom());
                h.setTo(destino);
                h.setSubject(asunto);
                h.setText(html, true);
                for (String rel : adjuntos) {
                    Path p = storage.resolve(rel);
                    if (Files.exists(p)) {
                        h.addAttachment(nombreVisible(p), new FileSystemResource(p));
                    }
                }
                mailSender.send(msg);
            } catch (MessagingException | RuntimeException e) {
                log.error("No fue posible enviar '{}' a {}: {}", asunto, destino, e.getMessage());
            }
        }
    }

    /** Los anexos se guardan como "<uuid>-<nombre>.pdf": se muestra solo el nombre original. */
    private String nombreVisible(Path p) {
        return p.getFileName().toString().replaceFirst("^[0-9a-fA-F-]{36}-", "");
    }

    private String plantilla(String titulo, String p1, String p2, String p3) {
        StringBuilder sb = new StringBuilder("<div style=\"font-family:Arial,sans-serif;font-size:14px;color:#222\">");
        sb.append("<h2 style=\"color:#1a3c6e\">").append(esc(titulo)).append("</h2>");
        for (String p : List.of(p1, p2, p3)) {
            if (!p.isBlank()) {
                sb.append("<p>").append(p).append("</p>");
            }
        }
        return sb.append("<hr/><p style=\"font-size:12px;color:#777\">Mensaje automático de SIGAC.</p></div>").toString();
    }

    private String esc(String s) {
        return HtmlSanitizer.escapeXml(s);
    }
}
