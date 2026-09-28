package com.briefai.exception.storage;

public class FileStorageException extends RuntimeException {
    public FileStorageException(String message) {
        super(message);
    }

    // There can be IOExceptions as we are dealing with files here.
    // In case of IOException we want to print the actual cause as well hence added second constructor
    public FileStorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
