package com.jch.gulimall.member.exception;

public class EmailExistException extends RuntimeException{
    public EmailExistException() {
        super("邮箱存在");
    }
}
