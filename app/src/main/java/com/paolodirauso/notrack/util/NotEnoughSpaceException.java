package com.paolodirauso.notrack.util;

public class NotEnoughSpaceException extends Exception {

    private static final long serialVersionUID = -1813052808134720258L;

    public NotEnoughSpaceException() {
    }

    public NotEnoughSpaceException(String msg) {
        super(msg);
    }
}