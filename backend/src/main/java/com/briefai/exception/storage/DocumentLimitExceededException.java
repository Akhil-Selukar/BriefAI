package com.briefai.exception.storage;

public class DocumentLimitExceededException extends RuntimeException{
    public DocumentLimitExceededException(String message){
        super(message);
    }
}
