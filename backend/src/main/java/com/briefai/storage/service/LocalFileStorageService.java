package com.briefai.storage.service;

import com.briefai.exception.storage.FileStorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {
    private final Path rootDirectory;

    public LocalFileStorageService(@Value("${app.storage.local.root-directory}") String rootDirectory) {
        this.rootDirectory = Path.of(rootDirectory).toAbsolutePath().normalize();
        initializeStorage();
    }

    private void initializeStorage() {
        try {
            Files.createDirectories(rootDirectory);
        } catch (IOException e) {
            throw new FileStorageException("Could not initialize file storage.", e);
        }
    }

    @Override
    public String store(InputStream inputStream, String originalFilename, Long userId) {

        String extension = extractExtension(originalFilename);
        String generatedFilename = UUID.randomUUID() + extension;

        String storageKey = userId + "/" + generatedFilename;

        Path destination = rootDirectory.resolve(storageKey).normalize();
        ensureInsideRoot(destination);

        try {
            Files.createDirectories(destination.getParent());
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
            return storageKey;
        } catch (IOException e) {
            throw new FileStorageException("Could not store file.", e);
        }
    }

    @Override
    public void delete(String storageKey) {
        Path target = rootDirectory.resolve(storageKey).normalize();
        ensureInsideRoot(target);

        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new FileStorageException("Could not delete file.", e);
        }
    }

    private String extractExtension(String filename) {
        if (filename == null) {
            return "";
        }

        int lastDotIndex = filename.lastIndexOf('.');

        if (lastDotIndex < 0) {
            return "";
        }

        return filename.substring(lastDotIndex).toLowerCase();
    }

    private void ensureInsideRoot(Path path) {
        if (!path.startsWith(rootDirectory)) {
            throw new FileStorageException("Invalid storage path.");
        }
    }
}
