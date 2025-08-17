package com.example.persistence.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Standard API response wrapper
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BaseResponse<T> {
    
    /**
     * Response status code
     */
    @Builder.Default
    private int code = 200;
    
    /**
     * Response message
     */
    private String message;
    
    /**
     * Response data
     */
    private T data;
    
    /**
     * Response timestamp
     */
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    /**
     * Request trace ID for debugging
     */
    private String traceId;
    
    /**
     * Additional metadata
     */
    private Object metadata;
    
    /**
     * Create successful response with data
     */
    public static <T> BaseResponse<T> success(T data) {
        return BaseResponse.<T>builder()
                .code(200)
                .message("Success")
                .data(data)
                .build();
    }
    
    /**
     * Create successful response with data and message
     */
    public static <T> BaseResponse<T> success(T data, String message) {
        return BaseResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * Create successful response with message only
     */
    public static <Void> BaseResponse<Void> success(String message) {
        return BaseResponse.<Void>builder()
                .code(200)
                .message(message)
                .build();
    }
    
    /**
     * Create error response
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return BaseResponse.<T>builder()
                .code(code)
                .message(message)
                .build();
    }
    
    /**
     * Create error response with data
     */
    public static <T> BaseResponse<T> error(int code, String message, T data) {
        return BaseResponse.<T>builder()
                .code(code)
                .message(message)
                .data(data)
                .build();
    }
    
    /**
     * Create bad request response
     */
    public static <T> BaseResponse<T> badRequest(String message) {
        return error(400, message);
    }
    
    /**
     * Create not found response
     */
    public static <T> BaseResponse<T> notFound(String message) {
        return error(404, message);
    }
    
    /**
     * Create internal server error response
     */
    public static <T> BaseResponse<T> serverError(String message) {
        return error(500, message);
    }
    
    /**
     * Create response with metadata
     */
    public static <T> BaseResponse<T> successWithMetadata(T data, String message, Object metadata) {
        return BaseResponse.<T>builder()
                .code(200)
                .message(message)
                .data(data)
                .metadata(metadata)
                .build();
    }
}