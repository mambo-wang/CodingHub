package com.iaihub.toolbox.exception;

public class UserNotFoundException extends BusinessException {

    public UserNotFoundException(String message) {
        super(404, message);
    }

    public UserNotFoundException(Long id) {
        super(404, "User not found with id: " + id);
    }
}
