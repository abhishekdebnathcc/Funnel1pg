package com.funnel1pg.utils;

import com.funnel1pg.config.ConfigReader;
import com.microsoft.playwright.*;

public class PlaywrightManager {

    private static final ThreadLocal<Playwright>     playwrightTL = new ThreadLocal<>();
    private static final ThreadLocal<Browser>        browserTL    = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> contextTL    = new ThreadLocal<>();
    private static final ThreadLocal<Page>           pageTL       = new ThreadLocal<>();

    public static void initBrowser() {
        Playwright playwright = Playwright.create();
        playwrightTL.set(playwright);

        BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                .setHeadless(ConfigReader.isHeadless())
                .setSlowMo(ConfigReader.getSlowMo());

        Browser browser;
        switch (ConfigReader.getBrowser().toLowerCase()) {
            case "firefox": browser = playwright.firefox().launch(opts); break;
            case "webkit":  browser = playwright.webkit().launch(opts);  break;
            default:        browser = playwright.chromium().launch(opts);
        }
        browserTL.set(browser);

        BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                .setViewportSize(1440, 900));
        contextTL.set(ctx);

        Page page = ctx.newPage();
        page.setDefaultTimeout(ConfigReader.getTimeout());
        pageTL.set(page);
    }

    public static Page           getPage()    { return pageTL.get(); }
    public static BrowserContext getContext() { return contextTL.get(); }

    public static void closeBrowser() {
        if (pageTL.get()       != null) { pageTL.get().close();       pageTL.remove(); }
        if (contextTL.get()    != null) { contextTL.get().close();    contextTL.remove(); }
        if (browserTL.get()    != null) { browserTL.get().close();    browserTL.remove(); }
        if (playwrightTL.get() != null) { playwrightTL.get().close(); playwrightTL.remove(); }
    }
}
