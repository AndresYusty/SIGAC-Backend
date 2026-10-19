package com.university.sigac.core.pdf;

import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Letterhead used by every PDF: page sheet and logo, replaceable by the administrator. */
@RestController
@RequestMapping("/api/v1/letterhead")
@RequiredArgsConstructor
public class LetterheadController {

    private final Letterhead letterhead;

    @GetMapping
    public Map<String, Boolean> status() {
        return Map.of("customSheet", letterhead.hasCustomSheet(), "logo", letterhead.hasLogo());
    }

    @GetMapping("/sheet")
    public ResponseEntity<byte[]> sheet() {
        return image(letterhead.sheet());
    }

    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        return image(letterhead.logo());
    }

    @PutMapping("/sheet")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> uploadSheet(@RequestParam("file") MultipartFile file) throws IOException {
        letterhead.saveSheet(file.getBytes());
        return status();
    }

    @PutMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> uploadLogo(@RequestParam("file") MultipartFile file) throws IOException {
        letterhead.saveLogo(file.getBytes());
        return status();
    }

    @DeleteMapping("/sheet")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> resetSheet() {
        letterhead.resetSheet();
        return status();
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Boolean> resetLogo() {
        letterhead.resetLogo();
        return status();
    }

    private static ResponseEntity<byte[]> image(byte[] data) {
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(Letterhead.mediaType(data)))
                .cacheControl(CacheControl.noStore()).body(data);
    }
}
