package com.funnel1pg.utils;

import com.funnel1pg.config.ConfigReader;
import com.microsoft.playwright.*;

public class PlaywrightManager {

    private static final ThreadLocal<Playwright>     playwrightTL = new ThreadLocal<>();
    private static final ThreadLocal<Browser>        browserTL    = new ThreadLocal<>();
    private static final ThreadLocal<BrowserContext> contextTL    = new ThreadLocal<>();
    private static final ThreadLocal<Page>           pageTL       = new ThreadLocal<>();

    /**
     * Captured order ID from the last CRM API response.
     * Set by OrderIdCaptor when the network response containing crm_response.order_id is received.
     */
    private static final ThreadLocal<String> capturedOrderId = new ThreadLocal<>();

    public static void setCapturedOrderId(String id)  { capturedOrderId.set(id); }
    public static String getCapturedOrderId()          { return capturedOrderId.get(); }
    public static void clearCapturedOrderId()          { capturedOrderId.remove(); }

    /**
     * Always launches a fresh browser instance for each scenario.
     * Never reuses an existing session.
     */
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

        // ── Network listener: capture order_id from every CRM API response ──
        // The checkout and upsell submissions return JSON like:
        //   {"success": true, "crm_response": {"order_id": "1234567", ...}}
        // We listen to ALL responses and extract order_id from any that contain it.
        page.onResponse(response -> {
            try {
                if (response.status() < 200 || response.status() >= 300) return;
                String contentType = response.headers().getOrDefault("content-type", "");
                if (!contentType.contains("json") && !contentType.contains("text")) return;
                String body = response.text();
                if (body == null || !body.contains("order_id")) return;

                String orderId = extractOrderId(body);
                if (orderId != null && !orderId.isEmpty() && !orderId.equals("null") && !orderId.equals("0")) {
                    if (getCapturedOrderId() == null || getCapturedOrderId().isEmpty()) {
                        setCapturedOrderId(orderId);
                        System.out.println("🔖 Order ID captured from network: " + orderId + " ← " + response.url());
                    }
                }
            } catch (Exception ignored) {
                // Network listener must never crash the test
            }
        });

        pageTL.set(page);
    }

    /** Extracts order_id value from a JSON string without requiring a JSON library. */
    private static String extractOrderId(String json) {
        // Try "order_id":"1234567" or "order_id":1234567
        String[] patterns = {"\"order_id\":\"", "\"order_id\":"};
        for (String pattern : patterns) {
            int idx = json.indexOf(pattern);
            if (idx < 0) continue;
            int start = idx + pattern.length();
            // Remove leading quote if present
            if (json.charAt(start) == '"') start++;
            int end = start;
            while (end < json.length() && json.charAt(end) != '"' && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
            String value = json.substring(start, end).trim();
            if (!value.isEmpty() && !value.equals("null") && !value.equals("0")) {
                return value;
            }
        }
        return null;
    }

    public static Page           getPage()    { return pageTL.get(); }
    public static BrowserContext getContext() { return contextTL.get(); }

    /**
     * Fully closes the browser instance and all associated resources.
     * Call this only for scenarios that should NOT leave the browser open.
     */
    public static void closeBrowser() {
        clearCapturedOrderId();
        if (pageTL.get()       != null) { try { pageTL.get().close();       } catch (Exception ignored) {} pageTL.remove(); }
        if (contextTL.get()    != null) { try { contextTL.get().close();    } catch (Exception ignored) {} contextTL.remove(); }
        if (browserTL.get()    != null) { try { browserTL.get().close();    } catch (Exception ignored) {} browserTL.remove(); }
        if (playwrightTL.get() != null) { try { playwrightTL.get().close(); } catch (Exception ignored) {} playwrightTL.remove(); }
    }
}
