package com.briefai.exception.storage;

public class DocumentTooLargeException extends RuntimeException {
    public DocumentTooLargeException(String message){
        super(message);
    }
}
