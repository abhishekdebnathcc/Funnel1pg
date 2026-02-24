package com.funnel1pg.pages;

import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;

public abstract class BasePage {

    protected final Page page;

    public BasePage(Page page) { this.page = page; }

    protected void click(String selector) {
        page.locator(selector).first().click();
    }

    protected void fill(String selector, String value) {
        page.locator(selector).first().fill(value);
    }

    protected void selectOption(String selector, String value) {
        page.locator(selector).first().selectOption(value);
    }

    protected void waitForVisible(String selector) {
        page.waitForSelector(selector,
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(15_000));
    }

    protected void waitForVisible(String selector, int timeoutMs) {
        page.waitForSelector(selector,
                new Page.WaitForSelectorOptions()
                        .setState(WaitForSelectorState.VISIBLE)
                        .setTimeout(timeoutMs));
    }

    protected boolean isVisible(String selector) {
        try { return page.locator(selector).first().isVisible(); }
        catch (Exception e) { return false; }
    }

    protected String getText(String selector) {
        try { return page.locator(selector).first().textContent().trim(); }
        catch (Exception e) { return ""; }
    }

    protected String getCurrentUrl() { return page.url(); }
}
