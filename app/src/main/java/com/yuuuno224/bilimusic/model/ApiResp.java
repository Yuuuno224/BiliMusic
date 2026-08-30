package com.yuuuno224.bilimusic.model;

public class ApiResp<T> {
    public int code;
    public String message;
    public T data;

    public boolean ok() {
        return code == 0;
    }
}
