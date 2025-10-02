package com.aichat.foundation.service;

import com.aichat.foundation.entity.FileAttachment;
import com.aichat.foundation.entity.Message;
import com.aichat.foundation.exception.FileStorageException;
import com.aichat.foundation.repository.FileAttachmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

    @Mock
    private FileAttachmentRepository fileAttachmentRepository;

    @InjectMocks
    private FileService fileService;

    @TempDir
    Path tempDir;

    private Message testMessage;
    private FileAttachment testFileAttachment;
    private UUID testFileId;

    @BeforeEach
    void setUp() {
        testFileId = UUID.randomUUID();
        testMessage = new Message();
        testMessage.setId(UUID.randomUUID());
        
        testFileAttachment = new FileAttachment(
            "test-image.jpg",
            "image/jpeg",
            tempDir.resolve("test-image.jpg").toString(),
            1024L,
            testMessage
        );
        testFileAttachment.setId(testFileId);

        // Set configuration properties
        ReflectionTestUtils.setField(fileService, "uploadDir", tempDir.toString());
        ReflectionTestUtils.setField(fileService, "maxFileSize", 10485760L); // 10MB
        ReflectionTestUtils.setField(fileService, "allowedTypes", "image/jpeg,image/png,image/gif,image/webp");
    }

    @Test
    void uploadFile_ShouldUploadSuccessfully_WhenValidFile() throws IOException {
        // Given
        byte[] fileContent = "test image content".getBytes();
        MockMultipartFile mockFile = new MockMultipartFile(
            "file",
            "test.jpg",
            "image/jpeg",
            fileContent
        );
        
        when(fileAttachmentRepository.save(any(FileAttachment.class))).thenReturn(testFileAttachment);

        // When
        var result = fileService.uploadFile(mockFile, testMessage);

        // Then
        assertNotNull(result);
        assertEquals("test.jpg", result.getFileName());
        assertEquals("image/jpeg", result.getFileType());
        assertEquals(fileContent.length, result.getFileSize());
        verify(fileAttachmentRepository).save(any(FileAttachment.class));
        
        // Verify file was actually written
        assertTrue(Files.exists(tempDir.resolve(result.getFilePath().substring(result.getFilePath().lastIndexOf('/') + 1))));
    }

    @Test
    void uploadFile_ShouldThrowException_WhenFileEmpty() {
        // Given
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[0]);

        // When & Then
        assertThrows(FileStorageException.class, () -> {
            fileService.uploadFile(emptyFile, testMessage);
        });
        
        verify(fileAttachmentRepository, never()).save(any());
    }

    @Test
    void uploadFile_ShouldThrowException_WhenFileTypeNotAllowed() {
        // Given
        MockMultipartFile invalidFile = new MockMultipartFile(
            "file",
            "test.txt",
            "text/plain",
            "test content".getBytes()
        );

        // When & Then
        assertThrows(FileStorageException.class, () -> {
            fileService.uploadFile(invalidFile, testMessage);
        });
        
        verify(fileAttachmentRepository, never()).save(any());
    }

    @Test
    void uploadFile_ShouldThrowException_WhenFileTooLarge() {
        // Given
        ReflectionTestUtils.setField(fileService, "maxFileSize", 100L); // 100 bytes
        MockMultipartFile largeFile = new MockMultipartFile(
            "file",
            "large.jpg",
            "image/jpeg",
            new byte[200] // 200 bytes
        );

        // When & Then
        assertThrows(FileStorageException.class, () -> {
            fileService.uploadFile(largeFile, testMessage);
        });
        
        verify(fileAttachmentRepository, never()).save(any());
    }

    @Test
    void getFileContent_ShouldReturnContent_WhenFileExists() throws IOException {
        // Given
        byte[] expectedContent = "test file content".getBytes();
        Path testFilePath = tempDir.resolve("test-file.jpg");
        Files.write(testFilePath, expectedContent);
        
        testFileAttachment.setFilePath(testFilePath.toString());
        when(fileAttachmentRepository.findById(testFileId)).thenReturn(Optional.of(testFileAttachment));

        // When
        byte[] result = fileService.getFileContent(testFileId);

        // Then
        assertArrayEquals(expectedContent, result);
        verify(fileAttachmentRepository).findById(testFileId);
    }

    @Test
    void getFileContent_ShouldThrowException_WhenFileNotFound() {
        // Given
        when(fileAttachmentRepository.findById(testFileId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(FileStorageException.class, () -> {
            fileService.getFileContent(testFileId);
        });
        
        verify(fileAttachmentRepository).findById(testFileId);
    }

    @Test
    void deleteFile_ShouldDeleteSuccessfully_WhenFileExists() throws IOException {
        // Given
        Path testFilePath = tempDir.resolve("test-delete.jpg");
        Files.write(testFilePath, "content".getBytes());
        
        testFileAttachment.setFilePath(testFilePath.toString());
        when(fileAttachmentRepository.findById(testFileId)).thenReturn(Optional.of(testFileAttachment));

        // When
        fileService.deleteFile(testFileId);

        // Then
        assertFalse(Files.exists(testFilePath));
        verify(fileAttachmentRepository).delete(testFileAttachment);
    }

    @Test
    void deleteFile_ShouldThrowException_WhenFileNotFound() {
        // Given
        when(fileAttachmentRepository.findById(testFileId)).thenReturn(Optional.empty());

        // When & Then
        assertThrows(FileStorageException.class, () -> {
            fileService.deleteFile(testFileId);
        });
        
        verify(fileAttachmentRepository, never()).delete(any());
    }

    @Test
    void fileExists_ShouldReturnTrue_WhenFileExists() {
        // Given
        when(fileAttachmentRepository.existsById(testFileId)).thenReturn(true);

        // When
        boolean result = fileService.fileExists(testFileId);

        // Then
        assertTrue(result);
        verify(fileAttachmentRepository).existsById(testFileId);
    }

    @Test
    void fileExists_ShouldReturnFalse_WhenFileNotExists() {
        // Given
        when(fileAttachmentRepository.existsById(testFileId)).thenReturn(false);

        // When
        boolean result = fileService.fileExists(testFileId);

        // Then
        assertFalse(result);
        verify(fileAttachmentRepository).existsById(testFileId);
    }

    @Test
    void getTotalFileSizeBySessionId_ShouldReturnTotalSize() {
        // Given
        UUID sessionId = UUID.randomUUID();
        when(fileAttachmentRepository.calculateTotalFileSizeBySessionId(sessionId)).thenReturn(5120L);

        // When
        long result = fileService.getTotalFileSizeBySessionId(sessionId);

        // Then
        assertEquals(5120L, result);
        verify(fileAttachmentRepository).calculateTotalFileSizeBySessionId(sessionId);
    }
}