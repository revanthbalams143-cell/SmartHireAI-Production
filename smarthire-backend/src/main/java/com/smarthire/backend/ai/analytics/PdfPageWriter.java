package com.smarthire.backend.ai.analytics;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;

/** Small PDF writer that safely rolls content onto new pages. */
public final class PdfPageWriter implements AutoCloseable {
    private final PDDocument document;
    private PDPage page;
    private PDPageContentStream stream;
    private final float margin = 50f;
    private final float width;
    private float y;

    public PdfPageWriter(PDDocument document) throws IOException {
        this.document = document;
        this.page = new PDPage();
        this.document.addPage(page);
        this.width = page.getMediaBox().getWidth() - (margin * 2f);
        this.y = page.getMediaBox().getHeight() - margin;
        this.stream = new PDPageContentStream(document, page);
    }

    public void heading(String text) throws IOException {
        writeLine(safe(text), new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14, 20);
    }

    public void title(String text) throws IOException {
        writeLine(safe(text), new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 18, 28);
    }

    public void line(String text) throws IOException {
        writeLine(safe(text), new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11, 15);
    }

    public void score(String label, Integer score) throws IOException {
        if (score != null) line(label + ": " + score + "/100");
    }

    public void bullet(String text) throws IOException {
        line("- " + safe(text));
    }

    public void paragraph(String text) throws IOException {
        if (text == null || text.isBlank()) return;
        String normalized = safe(text).replace('\n', ' ').replace('\r', ' ').trim();
        if (normalized.isEmpty()) return;
        String[] words = normalized.split("\\s+");
        StringBuilder current = new StringBuilder();
        PDType1Font font = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        for (String word : words) {
            String cleanWord = safe(word);
            if (cleanWord.isEmpty()) continue;
            String candidate = current.isEmpty() ? cleanWord : current + " " + cleanWord;
            float stringWidth;
            try {
                stringWidth = font.getStringWidth(candidate) / 1000f * 11f;
            } catch (Exception e) {
                candidate = sanitizeStrictAscii(candidate);
                stringWidth = font.getStringWidth(candidate) / 1000f * 11f;
            }
            if (stringWidth > width && !current.isEmpty()) {
                writeLine(current.toString(), font, 11, 15);
                current = new StringBuilder(cleanWord);
            } else {
                current = new StringBuilder(candidate);
            }
        }
        if (!current.isEmpty()) {
            writeLine(current.toString(), font, 11, 15);
        }
    }

    private void writeLine(String text, PDType1Font font, float fontSize, float leading) throws IOException {
        ensureSpace(leading + 4f);
        String clean = safe(text);
        if (clean.isEmpty()) return;
        try {
            stream.beginText();
            stream.setFont(font, fontSize);
            stream.newLineAtOffset(margin, y);
            stream.showText(clean);
            stream.endText();
        } catch (Exception ex) {
            String strict = sanitizeStrictAscii(clean);
            try {
                stream.beginText();
                stream.setFont(font, fontSize);
                stream.newLineAtOffset(margin, y);
                stream.showText(strict);
                stream.endText();
            } catch (Exception ignored) {
            }
        }
        y -= leading;
    }

    private void ensureSpace(float required) throws IOException {
        if (y - required >= 40f) return;
        if (stream != null) {
            stream.close();
        }
        page = new PDPage();
        document.addPage(page);
        stream = new PDPageContentStream(document, page);
        y = page.getMediaBox().getHeight() - margin;
    }

    private String safe(String value) {
        if (value == null) return "";
        return value.replace('\u2022', '-')
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201C', '"')
                .replace('\u201D', '"')
                .replaceAll("[^\\x20-\\x7E]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String sanitizeStrictAscii(String value) {
        if (value == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : value.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString().trim();
    }

    @Override
    public void close() throws IOException {
        if (stream != null) {
            stream.close();
            stream = null;
        }
    }
}
