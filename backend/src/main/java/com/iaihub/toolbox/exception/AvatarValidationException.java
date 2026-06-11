package com.iaihub.toolbox.exception;

public class AvatarValidationException extends BusinessException {

    public AvatarValidationException(String message) {
        super(400, message);
    }
}
