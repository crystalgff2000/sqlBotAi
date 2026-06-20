package com.sqlbot.dto;

import lombok.Data;

@Data
public class ResponseResult<T> {
    private int code;
    private String message;
    private T data;
    private long timestamp;
    
    public ResponseResult() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public static <T> ResponseResult<T> success(T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(200);
        result.setMessage("success");
        result.setData(data);
        return result;
    }
    
    public static <T> ResponseResult<T> success(String message, T data) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(200);
        result.setMessage(message);
        result.setData(data);
        return result;
    }
    
    public static <T> ResponseResult<T> error(String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(500);
        result.setMessage(message);
        return result;
    }
    
    public static <T> ResponseResult<T> error(int code, String message) {
        ResponseResult<T> result = new ResponseResult<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
