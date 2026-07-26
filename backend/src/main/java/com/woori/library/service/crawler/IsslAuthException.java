package com.woori.library.service.crawler;

/** issl.go.kr 로그인(아이디/비밀번호) 실패 시 발생. */
public class IsslAuthException extends RuntimeException {
    public IsslAuthException(String message) {
        super(message);
    }
}
