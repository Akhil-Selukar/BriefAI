package com.briefai.exception;

import com.briefai.exception.auth.EmailNotVerifiedException;
import com.briefai.exception.auth.InvalidCredentialsException;
import com.briefai.exception.conversation.ConversationNotFoundException;
import com.briefai.exception.document.DocumentNotFoundException;
import com.briefai.exception.otpExceptions.*;
import com.briefai.exception.rag.ChatModelResponseException;
import com.briefai.exception.storage.*;
import com.briefai.exception.document.IllegalDocumentStateException;
import com.briefai.exception.user.UserAlreadyExistsException;
import com.briefai.exception.user.UserNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.FieldError;

import java.time.LocalDateTime;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException e, HttpServletRequest request) {
        logger.error("Handling user already exist exception");
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "USER_ALREADY_EXISTS",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e, HttpServletRequest request) {
        logger.error("Handling validation error.");

        String message = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .findFirst()
                .orElse("Request validation failed.");

        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "VALIDATION_ERROR",
                message,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ErrorResponse> handleInvalidOtpException(InvalidOtpException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "INVALID_OTP",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(UserNotFoundException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "USER_NOT_FOUND",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(OtpNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOtpNotFoundException(OtpNotFoundException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "NO_OTP_FOUND_IN_SYSTEM",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler(EmailAlreadyVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailAlreadyVerifiedException(EmailAlreadyVerifiedException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "EMAIL_ALREADY_VERIFIED",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(OtpExpiredException.class)
    public ResponseEntity<ErrorResponse> handleOtpExpiredException(OtpExpiredException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "OTP_EXPIRED",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(OtpAttemptsExceededException.class)
    public ResponseEntity<ErrorResponse> handleOtpAttemptsExceededException(OtpAttemptsExceededException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "OTP_ATTEMPT_EXHAUSTED",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error);
    }

    @ExceptionHandler(OtpResendCoolDownException.class)
    public ResponseEntity<ErrorResponse> handleOtpResendCoolDownException(OtpResendCoolDownException e, HttpServletRequest request) {
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "OTP_RESEND_COOLDOWN",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(error);
    }

    @ExceptionHandler(EmailNotVerifiedException.class)
    public ResponseEntity<ErrorResponse> handleEmailNotVerifiedException(EmailNotVerifiedException e, HttpServletRequest request) {
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.FORBIDDEN.value(),
                "EMAIL_NOT_VERIFIED",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(error);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentialsException(InvalidCredentialsException e, HttpServletRequest request) {
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.UNAUTHORIZED.value(),
                "INVALID_CREDENTIALS",
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(error);
    }

    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<ErrorResponse> handleFileStorageException(FileStorageException e, HttpServletRequest request) {
        logger.error("Error occurred while storing file : {}", e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "FILE_STORAGE_ERROR",
                "An error occurred while storing the file.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error);
    }

    @ExceptionHandler(EmptyDocumentException.class)
    public ResponseEntity<ErrorResponse> handleEmptyDocumentException(EmptyDocumentException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "EMPTY_DOCUMENT",
                "The uploaded document is empty.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(DocumentLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleDocumentLimitExceededException(DocumentLimitExceededException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "DOCUMENT_LIMIT_EXCEEDED",
                "you have uploaded maximum allowed number of documents.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(UnsupportedDocumentTypeException.class)
    public ResponseEntity<ErrorResponse> handleUnsupportedDocumentTypeException(UnsupportedDocumentTypeException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "UNSUPPORTED_DOCUMENT_TYPE",
                "Only .pdf, .doc and .docx type of documents are allowed.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(error);
    }

    @ExceptionHandler(DocumentTooLargeException.class)
    public ResponseEntity<ErrorResponse> handleDocumentTooLargeException(DocumentTooLargeException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONTENT_TOO_LARGE.value(),
                "DOCUMENT_TOO_LARGE",
                "uploaded document is too large.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONTENT_TOO_LARGE)
                .body(error);
    }

    @ExceptionHandler(DocumentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleDocumentNotFoundException(DocumentNotFoundException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "DOCUMENT_NOT_FOUND",
                "Document not found",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }

    @ExceptionHandler({IllegalStateException.class, IllegalArgumentException.class})
    public ResponseEntity<ErrorResponse> handleIllegalStateException(IllegalStateException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.PRECONDITION_FAILED.value(),
                "DOCUMENT_PROCESSING_FAILED",
                "error while processing document",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.PRECONDITION_FAILED)
                .body(error);
    }

    @ExceptionHandler(IllegalDocumentStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalDocumentStateException(IllegalDocumentStateException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.CONFLICT.value(),
                "DOCUMENT_NOT_PROCESSABLE",
                "Document is not available for processing",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(error);
    }

    @ExceptionHandler(ChatModelResponseException.class)
    public ResponseEntity<ErrorResponse> handleChatModelResponseException(ChatModelResponseException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NO_CONTENT.value(),
                "CHAT_MODEL_DID_NOT_RESPOND",
                "Chat model did not respond, please try again",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NO_CONTENT)
                .body(error);
    }

    @ExceptionHandler(ConversationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleConversationNotFoundException(ConversationNotFoundException e, HttpServletRequest request){
        logger.error(e.getMessage());
        ErrorResponse error = new ErrorResponse(
                LocalDateTime.now(),
                HttpStatus.NOT_FOUND.value(),
                "CONVERSATION_NOT_FOUND",
                "Conversation not found.",
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(error);
    }
}
