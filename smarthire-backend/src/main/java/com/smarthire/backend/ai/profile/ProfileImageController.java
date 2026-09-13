package com.smarthire.backend.ai.profile;

import com.smarthire.backend.entity.User;
import com.smarthire.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import java.util.Arrays;
import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
public class ProfileImageController {

    private final UserRepository userRepository;
    private final String uploadDir;

    public ProfileImageController(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.uploadDir = "uploads/profiles";
    }

    @PostMapping("/{userId}/image")
    public ResponseEntity<Map<String, String>> uploadProfileImage(
            @PathVariable Long userId,
            @RequestParam("image") MultipartFile image, Authentication authentication) {
        if (!ownsUser(userId, authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "You may only modify your own profile"));
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Image file is required"));
        }

        try {
            // Validate image type
            if(image.getSize()>5L*1024*1024) return ResponseEntity.badRequest().body(Map.of("message","Profile image is too large (max 5 MB)."));
            String contentType = image.getContentType();
            if (contentType == null || !Set.of("image/jpeg","image/png","image/gif","image/webp").contains(contentType.toLowerCase())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Only JPG, PNG, GIF or WEBP images are allowed"));
            }
            byte[] header=image.getInputStream().readNBytes(12);
            if(!validImageSignature(contentType,header)) return ResponseEntity.badRequest().body(Map.of("message","Invalid image content."));

            // Create upload directory if needed
            Path dir = Paths.get(uploadDir);
            Files.createDirectories(dir);

            // Generate unique filename
            String extension = getExtension(contentType);
            String filename = "profile-" + userId + "-" + UUID.randomUUID() + extension;
            Path targetPath = dir.resolve(filename);

            // Save file
            image.transferTo(targetPath.toFile());

            // Update user record
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            user.setProfileImage("/uploads/profiles/" + filename);
            userRepository.save(user);

            return ResponseEntity.ok(Map.of(
                    "message", "Profile image uploaded successfully",
                    "imageUrl", user.getProfileImage()
            ));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Failed to save image: " + e.getMessage()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", e.getMessage()));
        }
    }

    @GetMapping("/{userId}/image")
    public ResponseEntity<Map<String, String>> getProfileImage(@PathVariable Long userId, Authentication authentication) {
        if (!ownsUser(userId, authentication)) return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("message", "Forbidden"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return ResponseEntity.ok(Map.of(
                "imageUrl", user.getProfileImage() == null ? "" : user.getProfileImage()
        ));
    }

    private boolean ownsUser(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) return false;
        return userRepository.findByEmail(authentication.getName()).map(User::getId).map(userId::equals).orElse(false);
    }


    private boolean validImageSignature(String contentType, byte[] h){
        if("image/png".equals(contentType)) return h.length>=8 && Arrays.equals(Arrays.copyOf(h,8), new byte[]{(byte)137,80,78,71,13,10,26,10});
        if("image/jpeg".equals(contentType)) return h.length>=3 && (h[0]&0xff)==0xff && (h[1]&0xff)==0xd8 && (h[2]&0xff)==0xff;
        if("image/gif".equals(contentType)) return h.length>=6 && new String(h,0,6).startsWith("GIF8");
        if("image/webp".equals(contentType)) return h.length>=12 && new String(h,0,4).equals("RIFF") && new String(h,8,4).equals("WEBP");
        return false;
    }

    private String getExtension(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}