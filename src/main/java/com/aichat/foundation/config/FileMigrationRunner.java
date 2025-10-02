package com.aichat.foundation.config;

import com.aichat.foundation.service.FileService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Data migration component to update file paths when upload directory changes
 */
@Component
public class FileMigrationRunner implements CommandLineRunner {
    
    private final FileService fileService;
    
    public FileMigrationRunner(FileService fileService) {
        this.fileService = fileService;
    }
    
    @Override
    public void run(String... args) throws Exception {
        // Check if migration is needed by looking for old upload directory pattern
        String oldUploadPath = "./uploads";
        String newUploadPath = "/Users/sansi/data/app/foundation/image";
        
        try {
            System.out.println("Checking for file path migration...");
            fileService.migrateFilePathsToNewDirectory(oldUploadPath, newUploadPath);
            System.out.println("File path migration completed.");
        } catch (Exception e) {
            System.err.println("Error during file path migration: " + e.getMessage());
            // Don't fail the application startup, just log the error
        }
    }
}