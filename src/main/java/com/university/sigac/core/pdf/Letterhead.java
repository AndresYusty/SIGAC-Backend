package com.university.sigac.core.pdf;

import com.university.sigac.core.exception.ApiException;
import com.university.sigac.core.storage.StorageService;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.springframework.stereotype.Component;

/**
 * Institutional page sheet drawn behind every document; templates reference it as {@code letterhead:usco}.
 * The administrator can upload another sheet and/or a logo. An uploaded logo replaces the USCO logo in the
 * header of the sheet.
 */
@Component
public class Letterhead {

    public static final String TOKEN = "letterhead:usco";

    private static final String SHEET_FILE = "branding/sheet";
    private static final String LOGO_FILE = "branding/logo";
    private static final int MAX_BYTES = 5 * 1024 * 1024;
    /** Header area of the 1224x1584 USCO sheet where its logo sits (pixels). */
    private static final Rectangle LOGO_AREA = new Rectangle(76, 28, 560, 96);

    private final StorageService storage;
    private final byte[] defaultSheet = resource("/sigac/templates/letterhead.png");
    private final byte[] sheetWithoutLogo = resource("/sigac/templates/letterhead-blank.png");

    public Letterhead(StorageService storage) {
        this.storage = storage;
    }

    /** Replaces the token with the embedded sheet so the PDF engine can draw it. */
    public String embed(String html) {
        if (html == null || !html.contains(TOKEN)) {
            return html;
        }
        return html.replace(TOKEN, dataUri(sheet()));
    }

    /** Sheet in use: the uploaded one or the USCO one, with the uploaded logo in the header. */
    public byte[] sheet() {
        if (!hasLogo()) {
            return hasCustomSheet() ? storage.read(SHEET_FILE) : defaultSheet;
        }
        return withLogo(hasCustomSheet() ? storage.read(SHEET_FILE) : sheetWithoutLogo, storage.read(LOGO_FILE));
    }

    /** Draws the logo in the header, scaled to fit the logo area and keeping its proportions. */
    private static byte[] withLogo(byte[] sheet, byte[] logo) {
        try {
            BufferedImage page = ImageIO.read(new ByteArrayInputStream(sheet));
            BufferedImage mark = ImageIO.read(new ByteArrayInputStream(logo));
            double k = page.getWidth() / 1224.0;
            double scale = Math.min(LOGO_AREA.width * k / mark.getWidth(), LOGO_AREA.height * k / mark.getHeight());
            int w = (int) Math.round(mark.getWidth() * scale);
            int h = (int) Math.round(mark.getHeight() * scale);
            int y = (int) Math.round(LOGO_AREA.y * k + (LOGO_AREA.height * k - h) / 2);
            BufferedImage out = new BufferedImage(page.getWidth(), page.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g = out.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, out.getWidth(), out.getHeight());
            g.drawImage(page, 0, 0, null);
            g.drawImage(mark, (int) Math.round(LOGO_AREA.x * k), y, w, h, null);
            g.dispose();
            ByteArrayOutputStream png = new ByteArrayOutputStream();
            ImageIO.write(out, "png", png);
            return png.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Could not compose the letterhead", e);
        }
    }

    public byte[] logo() {
        if (!hasLogo()) {
            throw ApiException.notFound("No logo has been uploaded");
        }
        return storage.read(LOGO_FILE);
    }

    public boolean hasCustomSheet() {
        return storage.exists(SHEET_FILE);
    }

    public boolean hasLogo() {
        return storage.exists(LOGO_FILE);
    }

    public void saveSheet(byte[] image) {
        storage.save(SHEET_FILE, requireImage(image));
    }

    public void saveLogo(byte[] image) {
        storage.save(LOGO_FILE, requireImage(image));
    }

    public void resetSheet() {
        storage.deleteQuietly(SHEET_FILE);
    }

    public void resetLogo() {
        storage.deleteQuietly(LOGO_FILE);
    }

    public static String mediaType(byte[] image) {
        return image.length > 2 && (image[0] & 0xFF) == 0xFF && (image[1] & 0xFF) == 0xD8 ? "image/jpeg" : "image/png";
    }

    private static String dataUri(byte[] image) {
        return "data:" + mediaType(image) + ";base64," + Base64.getEncoder().encodeToString(image);
    }

    private static byte[] requireImage(byte[] data) {
        if (data == null || data.length == 0) {
            throw ApiException.badRequest("Select an image");
        }
        if (data.length > MAX_BYTES) {
            throw ApiException.payloadTooLarge("The image cannot exceed 5 MB");
        }
        String type = mediaType(data);
        boolean png = data.length > 4 && data[1] == 'P' && data[2] == 'N' && data[3] == 'G';
        if (!png && !type.equals("image/jpeg")) {
            throw ApiException.badRequest("Only PNG or JPG images are allowed");
        }
        try {
            if (ImageIO.read(new ByteArrayInputStream(data)) == null) {
                throw ApiException.badRequest("The file is not a valid image");
            }
        } catch (IOException e) {
            throw ApiException.badRequest("The file is not a valid image");
        }
        return data;
    }

    private static byte[] resource(String path) {
        try (InputStream in = Letterhead.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Missing resource " + path);
            }
            return in.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + path, e);
        }
    }
}
