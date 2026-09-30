package com.briefai.exception.rag;

public class ChatModelResponseException extends RuntimeException{
    public ChatModelResponseException(String message){
        super(message);
    }
}
