package com.refconstructionopc.response;

public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    public ApiResponse() {}
    public ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "OK", data);
    }

    public static <T> ApiResponse<T> success(int status, String message, T data) {
        return new ApiResponse<>(status, message, data);
    }

    public static <T> ApiResponse<T> error(String message, T details) {
        return new ApiResponse<>(400, message, details);
    }

    public static <T> ApiResponse<T> error(int status, String message, T details) {
        return new ApiResponse<>(status, message, details);
    }
    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }
}