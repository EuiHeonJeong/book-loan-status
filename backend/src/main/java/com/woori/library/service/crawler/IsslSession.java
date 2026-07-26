package com.woori.library.service.crawler;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;

/** 로그인된 issl.go.kr 브라우저 세션. 스크레이핑이 끝나면 반드시 close()로 정리한다. */
public class IsslSession implements AutoCloseable {

    private final Playwright playwright;
    private final Browser browser;
    private final BrowserContext context;
    private final Page page;

    public IsslSession(Playwright playwright, Browser browser, BrowserContext context, Page page) {
        this.playwright = playwright;
        this.browser = browser;
        this.context = context;
        this.page = page;
    }

    public Page page() {
        return page;
    }

    public BrowserContext context() {
        return context;
    }

    @Override
    public void close() {
        try {
            browser.close();
        } finally {
            playwright.close();
        }
    }
}
