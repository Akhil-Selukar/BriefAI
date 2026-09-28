package com.briefai.exception.storage;

public class UnsupportedDocumentTypeException extends RuntimeException {
    public UnsupportedDocumentTypeException(String message){
        super(message);
    }
}
