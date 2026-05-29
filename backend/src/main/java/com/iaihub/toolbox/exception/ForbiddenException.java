package com.iaihub.toolbox.exception;

public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(403, message);
    }
}
