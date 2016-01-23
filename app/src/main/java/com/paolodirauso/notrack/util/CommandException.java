package com.paolodirauso.notrack.util;

public class CommandException extends Exception {

    private static final long serialVersionUID = -4014185620880841310L;

    public CommandException() {
    }

    public CommandException(String msg) {
        super(msg);
    }
}