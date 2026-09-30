package com.briefai.storage.service;

import com.briefai.storage.service.LocalFileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceTest {
    @TempDir
    Path tempDirectory;
    private LocalFileStorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageService(tempDirectory.toString());
    }

    @Test
    void store_shouldStoreFileAndReturnStorageKey() throws IOException {

        byte[] content = "Test document content for testing.".getBytes();

        String storageKey = storageService.store(new ByteArrayInputStream(content), "test.pdf", 1L);

        assertNotNull(storageKey);
        assertTrue(storageKey.startsWith("1/"));
        assertTrue(storageKey.endsWith(".pdf"));

        Path storedFile = tempDirectory.resolve(storageKey);
        assertTrue(Files.exists(storedFile));
        assertArrayEquals(content, Files.readAllBytes(storedFile));
    }

    @Test
    void store_shouldPreserveFileExtension() throws IOException{
        byte[] content = "Test document content for testing.".getBytes();

        String storageKey = storageService.store(new ByteArrayInputStream(content), "test.pdf", 1L);

        assertTrue(storageKey.endsWith(".pdf"));
    }

    @Test
    void store_shouldCreateSeparateUserDirectory() {
        byte[] content = "Test document content for testing.".getBytes();

        String storageKey = storageService.store(new ByteArrayInputStream(content), "test.pdf", 1L);
        Path userDirectory = tempDirectory.resolve("1");

        assertTrue(Files.exists(userDirectory));
        assertTrue(Files.isDirectory(userDirectory));
        assertTrue(storageKey.startsWith("1/"));    // This folder path for this file start's with userId with / so for each user we have a folder structure
        assertTrue(Files.exists(tempDirectory.resolve(storageKey)));   // Check if the file exists or not.
    }

    @Test
    void delete_shouldDeleteStoredFile() {
        byte[] content = "Test document content for testing.".getBytes();

        String storageKey = storageService.store(new ByteArrayInputStream(content), "test.pdf", 1L);
        Path storedFile = tempDirectory.resolve(storageKey);

        assertTrue(Files.exists(storedFile));

        storageService.delete(storageKey);

        assertFalse(Files.exists(storedFile));
    }
}