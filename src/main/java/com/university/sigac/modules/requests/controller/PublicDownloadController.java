package com.university.sigac.modules.requests.controller;

import com.university.sigac.modules.requests.service.AgendaService;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unauthenticated downloads opened from citation emails. */
@RestController
@RequestMapping("/api/v1/public")
@RequiredArgsConstructor
public class PublicDownloadController {

    private final AgendaService agenda;

    @GetMapping("/soportes/{token}")
    public ResponseEntity<byte[]> supports(@PathVariable String token) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/zip"))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename("Soportes del orden del día.zip", StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(agenda.downloadSupportsByToken(token));
    }
}
