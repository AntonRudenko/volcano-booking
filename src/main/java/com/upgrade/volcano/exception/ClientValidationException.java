package com.upgrade.volcano.exception;

public class ClientValidationException extends BaseException {

    public ClientValidationException(String message) {
        super(message, ErrorCode.VALIDATION_ERROR);
    }
}
