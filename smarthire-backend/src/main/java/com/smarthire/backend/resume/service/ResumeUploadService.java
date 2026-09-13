package com.smarthire.backend.resume.service;

import com.smarthire.backend.resume.dto.ResumeUploadResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class ResumeUploadService {

    @Value("${app.upload.dir:uploads/resumes}")
    private String uploadDir;

    public ResumeUploadResponse uploadResume(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ResumeUploadResponse(false, null, null, "No file selected. Please choose a PDF file to upload.");
        }

        String originalFileName = file.getOriginalFilename();
        if (file.getSize() > 20L * 1024 * 1024) {
            return new ResumeUploadResponse(false, null, null, "Resume file is too large. Maximum allowed size is 20 MB.");
        }
        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
            return new ResumeUploadResponse(false, null, null, "Only PDF files are allowed. Please upload a .pdf file.");
        }
        String contentType=file.getContentType();
        if(contentType!=null && !contentType.equalsIgnoreCase("application/pdf") && !contentType.equalsIgnoreCase("application/octet-stream")) {
            return new ResumeUploadResponse(false, null, null, "The uploaded file is not recognized as a PDF.");
        }
        byte[] header = file.getInputStream().readNBytes(5);
        if(header.length < 5 || header[0] != '%' || header[1] != 'P' || header[2] != 'D' || header[3] != 'F' || header[4] != '-') {
            return new ResumeUploadResponse(false, null, null, "The uploaded file is not a valid PDF.");
        }

        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadPath);

        String safeName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        String storedFileName = UUID.randomUUID().toString() + "_" + safeName;
        Path targetLocation = uploadPath.resolve(storedFileName);

        Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

        String filePath = targetLocation.toString();

        return new ResumeUploadResponse(
                true,
                originalFileName,
                filePath,
                "Resume uploaded successfully."
        );
    }
}