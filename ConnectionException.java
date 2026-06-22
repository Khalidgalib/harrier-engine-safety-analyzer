package com.example.project_prototype.exception;

public class ConnectionException extends Exception {
    public ConnectionException(String message) {
        super(message);

    }
    public ConnectionException (String message , Throwable cause){
        super(message , cause);
    }
}
