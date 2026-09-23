package com.example.smartdesk.exception;

public class CutoffExceededException extends RuntimeException {
    public CutoffExceededException(String message) {
        super(message);
    }
}
