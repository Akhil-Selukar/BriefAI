package com.briefai.storage.service;

import java.io.InputStream;

public interface FileStorageService {
    String store(InputStream inputStream, String originalFilename, Long userId);
    void delete(String storageKey);
}
