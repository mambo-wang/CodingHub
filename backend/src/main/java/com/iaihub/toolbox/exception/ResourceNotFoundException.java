package com.iaihub.toolbox.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(404, message);
    }

    public ResourceNotFoundException(String resourceName, Long id) {
        super(404, resourceName + " not found with id: " + id);
    }
}
