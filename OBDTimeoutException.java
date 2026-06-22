package com.example.project_prototype.exception;

public class OBDTimeoutException extends RuntimeException {
    private final String timeOutCommand;

    public OBDTimeoutException(String message) {
        super(message);
        this.timeOutCommand = "UNKNOWN";
    }
    public OBDTimeoutException(String message , String command){
        super(message);
        this.timeOutCommand = command;
    }
    public OBDTimeoutException(String message , String command , Throwable cause){
        super(message , cause);
        this.timeOutCommand = command;

    }
    public  String getTimeOutCommand(){
        return this.timeOutCommand ;
    }

}
