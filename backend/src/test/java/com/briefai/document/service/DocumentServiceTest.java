package com.briefai.document.service;

import com.briefai.document.dto.DocumentResponse;
import com.briefai.document.entity.Document;
import com.briefai.document.entity.DocumentStatus;
import com.briefai.document.repository.DocumentRepository;
import com.briefai.exception.storage.document.DocumentNotFoundException;
import com.briefai.exception.storage.*;
import com.briefai.exception.user.UserNotFoundException;
import com.briefai.storage.service.FileStorageService;
import com.briefai.user.entity.User;
import com.briefai.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {
    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FileStorageService fileStorageService;
    private DocumentService documentService;
    private User user;

    @BeforeEach
    void setUp() {
        documentService = new DocumentService(documentRepository, userRepository, fileStorageService,
                10 * 1024 * 1024, 5);

        user = new User("Penny", "penny@test.com", "23asfadsaS#fgds");
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    void uploadDocument_shouldUploadValidDocument() {

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf",
                "Test document content".getBytes());

        when(documentRepository.countByUserId(1L)).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileStorageService.store(any(InputStream.class), eq("test.pdf"), eq(1L))).thenReturn("1/123jh-sdvsd-123.pdf");
        when(documentRepository.save(any(Document.class))).thenAnswer(invocation -> {
            Document document = invocation.getArgument(0);
            ReflectionTestUtils.setField(document, "id", 100L);
            return document;
        });

        DocumentResponse response = documentService.uploadDocument(1L, file);

        assertEquals(100L, response.getId());
        assertEquals("test.pdf", response.getOriginalName());
        assertEquals("application/pdf", response.getContentType());
        assertEquals(file.getSize(), response.getSizeBytes());
        assertNull(response.getPageCount());
        assertEquals(DocumentStatus.UPLOADED, response.getStatus());

        verify(fileStorageService).store(any(InputStream.class), eq("test.pdf"), eq(1L));
        verify(documentRepository).save(any(Document.class));
    }

    @Test
    void uploadDocument_shouldRejectEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", new byte[0]);

        assertThrows(EmptyDocumentException.class, () -> documentService.uploadDocument(1L, file));

        verifyNoInteractions(userRepository, fileStorageService);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_shouldRejectFileLargerThan10MB() {
        MultipartFile file = mock(MultipartFile.class);

        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn(10L * 1024 * 1024 + 1); // greater than 10Mb

        assertThrows(DocumentTooLargeException.class, () -> documentService.uploadDocument(1L, file));

        verifyNoInteractions(userRepository, fileStorageService);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_shouldRejectUnsupportedType() {

        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", "Test image".getBytes());

        assertThrows(UnsupportedDocumentTypeException.class, () -> documentService.uploadDocument(1L, file));

        verifyNoInteractions(userRepository, fileStorageService);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_shouldRejectWhenUserHasFiveDocuments() {

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test document".getBytes());

        when(documentRepository.countByUserId(1L)).thenReturn(5L);  // already have 5 documents stored
        assertThrows(DocumentLimitExceededException.class, () -> documentService.uploadDocument(1L, file));

        verify(documentRepository).countByUserId(1L);
        verifyNoInteractions(userRepository, fileStorageService);
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_shouldRejectWhenUserDoesNotExist() {

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test document".getBytes());

        when(documentRepository.countByUserId(1L)).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());     // no user present

        assertThrows(UserNotFoundException.class, () -> documentService.uploadDocument(1L, file));

        verify(fileStorageService, never()).store(any(), anyString(), anyLong());
        verify(documentRepository, never()).save(any());
    }

    @Test
    void uploadDocument_shouldDeleteStoredFileWhenDatabaseSaveFails() {
        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test document".getBytes());

        when(documentRepository.countByUserId(1L)).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileStorageService.store(any(InputStream.class), eq("test.pdf"), eq(1L))).thenReturn("1/123-afa23-123s.pdf");

        RuntimeException databaseFailure = new RuntimeException("Database unavailable");

        when(documentRepository.save(any(Document.class))).thenThrow(databaseFailure);

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> documentService.uploadDocument(1L, file));

        assertSame(databaseFailure, thrown);
        verify(fileStorageService).delete("1/123-afa23-123s.pdf");
    }

    @Test
    void uploadDocument_shouldPreserveOriginalExceptionWhenCleanupFails() {

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "document".getBytes());

        when(documentRepository.countByUserId(1L)).thenReturn(0L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(fileStorageService.store(any(InputStream.class), eq("test.pdf"), eq(1L))).thenReturn("1/123d-bdfgf-54s.pdf");

        RuntimeException databaseFailure = new RuntimeException("Database unavailable");
        when(documentRepository.save(any(Document.class))).thenThrow(databaseFailure);

        FileStorageException cleanupFailure = new FileStorageException("Could not delete stored file.");
        doThrow(cleanupFailure).when(fileStorageService).delete("1/123d-bdfgf-54s.pdf");

        RuntimeException thrown = assertThrows(RuntimeException.class, () -> documentService.uploadDocument(1L, file));

        assertSame(databaseFailure, thrown);
        verify(fileStorageService).delete("1/123d-bdfgf-54s.pdf");
    }

    @Test
    void getDocuments_shouldReturnOnlyUserDocuments() {

        Document document1 = new Document(user, "test1.pdf", "1/test1.pdf", "application/pdf", 100L);
        Document document2 = new Document(user, "test2.pdf", "1/two.pdf", "application/pdf", 200L);
        ReflectionTestUtils.setField(document1, "id", 1L);
        ReflectionTestUtils.setField(document2, "id", 2L);

        when(documentRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(document2, document1));

        List<DocumentResponse> result = documentService.getDocuments(1L);

        assertEquals(2, result.size());
        assertEquals(2L, result.get(0).getId());
        assertEquals(1L, result.get(1).getId());
        verify(documentRepository).findAllByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getDocument_shouldReturnOwnedDocument() {

        Document document = new Document(user, "test1.pdf", "1/research.pdf", "application/pdf", 100L);
        ReflectionTestUtils.setField(document, "id", 100L);

        when(documentRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(document));
        DocumentResponse response = documentService.getDocument(1L, 100L);

        assertEquals(100L, response.getId());
        assertEquals("test1.pdf", response.getOriginalName());

        verify(documentRepository).findByIdAndUserId(100L, 1L);
    }

    @Test
    void getDocument_shouldThrowErrorWhenDocumentDoesNotBelongToUser() {
        when(documentRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());
        assertThrows(DocumentNotFoundException.class, () -> documentService.getDocument(1L, 200L));
    }

    @Test
    void deleteDocument_shouldDeleteOwnedDocumentAndStoredFile() {
        Document document = new Document(user, "test1.pdf", "1/generated.pdf", "application/pdf", 100L);
        ReflectionTestUtils.setField(document, "id", 100L);

        when(documentRepository.findByIdAndUserId(100L, 1L)).thenReturn(Optional.of(document));

        documentService.deleteDocument(1L, 100L);

        verify(fileStorageService).delete("1/generated.pdf");
        verify(documentRepository).delete(document);
    }

    @Test
    void deleteDocument_shouldThrowWhenDocumentDoesNotBelongToUser() {
        when(documentRepository.findByIdAndUserId(200L, 1L)).thenReturn(Optional.empty());

        assertThrows(DocumentNotFoundException.class, () -> documentService.deleteDocument(1L, 200L));
        verifyNoInteractions(fileStorageService);
        verify(documentRepository, never()).delete(any());
    }


}