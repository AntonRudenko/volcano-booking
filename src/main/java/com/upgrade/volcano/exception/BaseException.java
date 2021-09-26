package com.upgrade.volcano.exception;

public class BaseException extends RuntimeException {
    private ErrorCode code;
    private String message;

    public BaseException(String message, ErrorCode code) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public BaseException(String message, Throwable cause, ErrorCode code) {
        super(message, cause);
        this.code = code;
        this.message = message;
    }

    public ErrorCode getCode() {
        return code;
    }

    public void setCode(ErrorCode code) {
        this.code = code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
