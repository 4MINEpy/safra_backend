package com.safra.safra.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${app.base-url:http://localhost:8087}")
    private String baseUrl;

    private Path uploadPath;

    // Allowed image extensions
    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList(
            "jpg", "jpeg", "png", "gif", "webp"
    );

    // Max file size: 5MB
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024;

    @PostConstruct
    public void init() {
        try {
            uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(uploadPath);
            
            // Create subdirectories for different types
            Files.createDirectories(uploadPath.resolve("profiles"));
            Files.createDirectories(uploadPath.resolve("cars"));
            
            log.info("📁 File storage initialized at: {}", uploadPath);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory!", e);
        }
    }

    /**
     * Store a profile picture
     * @param file the uploaded file
     * @param userId the user's ID (for naming)
     * @return the URL to access the stored image
     */
    public String storeProfilePicture(MultipartFile file, Long userId) throws IOException {
        validateFile(file);
        
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        
        // Generate unique filename: profile_{userId}_{uuid}.{ext}
        String newFilename = String.format("profile_%d_%s.%s", 
                userId, UUID.randomUUID().toString().substring(0, 8), extension);
        
        Path targetPath = uploadPath.resolve("profiles").resolve(newFilename);
        
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.info("✅ Profile picture stored: {}", newFilename);
        
        // Return the URL to access this file
        return baseUrl + "/uploads/profiles/" + newFilename;
    }

    /**
     * Store a car image
     * @param file the uploaded file
     * @param carId the car's ID
     * @return the URL to access the stored image
     */
    public String storeCarImage(MultipartFile file, Long carId) throws IOException {
        validateFile(file);
        
        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String extension = getFileExtension(originalFilename);
        
        String newFilename = String.format("car_%d_%s.%s", 
                carId, UUID.randomUUID().toString().substring(0, 8), extension);
        
        Path targetPath = uploadPath.resolve("cars").resolve(newFilename);
        
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        
        log.info("✅ Car image stored: {}", newFilename);
        
        return baseUrl + "/uploads/cars/" + newFilename;
    }

    /**
     * Delete a file by its URL
     */
    public boolean deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isEmpty()) {
            return false;
        }
        
        try {
            // Extract the relative path from the URL
            String relativePath = fileUrl.replace(baseUrl + "/uploads/", "");
            Path filePath = uploadPath.resolve(relativePath);
            
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("🗑️ File deleted: {}", relativePath);
                return true;
            }
        } catch (IOException e) {
            log.error("❌ Failed to delete file: {}", fileUrl, e);
        }
        return false;
    }

    /**
     * Validate the uploaded file
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty or null");
        }
        
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("File size exceeds maximum limit of 5MB");
        }
        
        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            throw new IllegalArgumentException("Filename is invalid");
        }
        
        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException(
                    "File type not allowed. Allowed types: " + String.join(", ", ALLOWED_EXTENSIONS));
        }
        
        // Check MIME type as well
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0) {
            throw new IllegalArgumentException("File has no extension");
        }
        return filename.substring(dotIndex + 1);
    }

    /**
     * Get the upload path
     */
    public Path getUploadPath() {
        return uploadPath;
    }
}
