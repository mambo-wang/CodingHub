package com.iaihub.toolbox.exception;

public class FileValidationException extends BusinessException {

    public FileValidationException(String message) {
        super(400, message);
    }
}
