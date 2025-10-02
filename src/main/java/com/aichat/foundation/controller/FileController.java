package com.aichat.foundation.controller;

import com.aichat.foundation.dto.FileAttachmentDto;
import com.aichat.foundation.service.FileService;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {
    
    private final FileService fileService;
    
    public FileController(FileService fileService) {
        this.fileService = fileService;
    }
    
    /**
     * Get file content by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Resource> getFile(@PathVariable UUID id) {
        try {
            FileAttachmentDto fileInfo = fileService.getFileInfo(id);
            byte[] fileContent = fileService.getFileContent(id);
            
            ByteArrayResource resource = new ByteArrayResource(fileContent);
            
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileInfo.getFileType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                       "attachment; filename=\"" + fileInfo.getFileName() + "\"")
                .body(resource);
                
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Get file information (metadata only)
     */
    @GetMapping("/{id}/info")
    public ResponseEntity<FileAttachmentDto> getFileInfo(@PathVariable UUID id) {
        try {
            FileAttachmentDto fileInfo = fileService.getFileInfo(id);
            return ResponseEntity.ok(fileInfo);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Delete a file
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFile(@PathVariable UUID id) {
        try {
            fileService.deleteFile(id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
    
    /**
     * Upload a standalone file (not associated with a message yet)
     */
    @PostMapping("/upload")
    public ResponseEntity<UploadResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            // This would typically be used for temporary uploads
            // For now, return basic file information
            return ResponseEntity.ok(new UploadResponse(
                "File uploaded successfully",
                file.getOriginalFilename(),
                file.getSize(),
                file.getContentType()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new UploadResponse("Upload failed: " + e.getMessage(), null, 0L, null));
        }
    }
    
    // Response DTO for file upload
    public static class UploadResponse {
        private String message;
        private String fileName;
        private Long fileSize;
        private String fileType;
        
        public UploadResponse(String message, String fileName, Long fileSize, String fileType) {
            this.message = message;
            this.fileName = fileName;
            this.fileSize = fileSize;
            this.fileType = fileType;
        }
        
        // Getters and setters
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getFileName() {
            return fileName;
        }
        
        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
        
        public Long getFileSize() {
            return fileSize;
        }
        
        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }
        
        public String getFileType() {
            return fileType;
        }
        
        public void setFileType(String fileType) {
            this.fileType = fileType;
        }
    }
}