package com.bilimusic.app._02_model;

public class ApiResp<T> {
    public int code;
    public String message;
    public T data;

    public boolean ok() {
        return code == 0;
    }
}
