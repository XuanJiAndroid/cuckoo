package com.guoxiaoxing.cuckoo.exception;

/**
 * Sensors Analytics 返回数据收集异常
 */
public class ResponseErrorException extends Exception {

    public ResponseErrorException(String error) {
        super(error);
    }

    public ResponseErrorException(Throwable throwable) {
        super(throwable);
    }

}
