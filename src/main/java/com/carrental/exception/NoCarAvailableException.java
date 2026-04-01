package com.carrental.exception;

public class NoCarAvailableException extends RuntimeException {
    public NoCarAvailableException(String message) {
        super(message);
    }
}
