package com.briefai.exception.storage;

public class EmptyDocumentException extends RuntimeException {
    public EmptyDocumentException(String message){
        super(message);
    }
}
