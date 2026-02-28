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
     * Initialises the browser session.
     *
     * Default mode  : launches a fresh Chromium/Firefox/WebKit instance.
     * CDP mode      : connects to an already-running Chrome via Chrome DevTools Protocol.
     *
     * Activate CDP mode from the command line:
     *   mvn test -Dcdp=true                        # connects to localhost:9222
     *   mvn test -Dcdp=true -Dcdp.port=9333        # custom port
     *
     * To start Chrome ready for CDP:
     *   /Applications/Google\ Chrome.app/Contents/MacOS/Google\ Chrome \
     *       --remote-debugging-port=9222 --no-first-run --no-default-browser-check
     */
    public static void initBrowser() {
        Playwright playwright = Playwright.create();
        playwrightTL.set(playwright);

        boolean cdpMode = Boolean.parseBoolean(System.getProperty("cdp", "false"));

        if (cdpMode) {
            // ── CDP mode: attach to existing Chrome ──────────────────────────
            int cdpPort = Integer.parseInt(System.getProperty("cdp.port", "9222"));
            String cdpUrl = "http://localhost:" + cdpPort;
            System.out.println("🔌 CDP mode – connecting to existing Chrome at " + cdpUrl);
            try {
                Browser browser = playwright.chromium().connectOverCDP(cdpUrl);
                browserTL.set(browser);

                BrowserContext ctx = browser.contexts().isEmpty()
                        ? browser.newContext()
                        : browser.contexts().get(0);
                // Suppress save prompts in CDP mode too
                suppressSavePrompts(ctx);
                contextTL.set(ctx);

                Page page = ctx.pages().isEmpty() ? ctx.newPage() : ctx.pages().get(0);
                page.setDefaultTimeout(ConfigReader.getTimeout());
                attachNetworkListener(page);
                pageTL.set(page);
                System.out.println("✓ CDP connected – using existing browser session");
            } catch (Exception e) {
                throw new RuntimeException(
                    "CDP connection failed on port " + cdpPort + ". " +
                    "Start Chrome with: --remote-debugging-port=" + cdpPort, e);
            }
        } else {
            // ── Normal mode: launch fresh browser ────────────────────────────
            BrowserType.LaunchOptions opts = new BrowserType.LaunchOptions()
                    .setHeadless(ConfigReader.isHeadless())
                    .setSlowMo(ConfigReader.getSlowMo())
                    .setArgs(java.util.Arrays.asList(
                        "--disable-save-password-bubble",
                        "--disable-features=AutofillServerCommunication",
                        "--disable-autofill-keyboard-accessory-view",
                        "--no-default-browser-check",
                        "--no-first-run",
                        "--disable-default-apps",
                        "--disable-infobars",
                        "--disable-notifications",
                        "--disable-translate",
                        "--password-store=basic"
                    ));

            Browser browser;
            switch (ConfigReader.getBrowser().toLowerCase()) {
                case "firefox": browser = playwright.firefox().launch(opts); break;
                case "webkit":  browser = playwright.webkit().launch(opts);  break;
                default:        browser = playwright.chromium().launch(opts);
            }
            browserTL.set(browser);

            BrowserContext ctx = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1440, 900));
            // Suppress credential/payment save prompts at the context level
            suppressSavePrompts(ctx);
            contextTL.set(ctx);

            Page page = ctx.newPage();
            page.setDefaultTimeout(ConfigReader.getTimeout());
            attachNetworkListener(page);
            pageTL.set(page);
        }
    }

    /**
     * Suppresses browser save-card, save-address, save-password dialogs
     * by granting/revoking the right permissions and injecting page-level
     * autofill/autocomplete disablement.
     */
    private static void suppressSavePrompts(BrowserContext ctx) {
        try {
            // Deny notifications (prevents address-bar prompts)
            ctx.grantPermissions(java.util.Collections.emptyList());

            // Inject script into every page that disables autocomplete and
            // removes Chrome's autofill/credential-manager hooks before the
            // page's own scripts run.
            ctx.addInitScript(
                "(function() {" +
                "  // Prevent credential-manager save prompt" +
                "  if (window.PasswordCredential) {" +
                "    Object.defineProperty(window, 'PasswordCredential', {get: function(){return undefined;}});" +
                "  }" +
                "  // Prevent PaymentRequest save prompt" +
                "  if (window.PaymentRequest) {" +
                "    Object.defineProperty(window, 'PaymentRequest', {get: function(){return undefined;}});" +
                "  }" +
                "  // Force autocomplete=off on all inputs after DOM settles" +
                "  document.addEventListener('DOMContentLoaded', function() {" +
                "    document.querySelectorAll('input, form').forEach(function(el) {" +
                "      el.setAttribute('autocomplete', 'off');" +
                "    });" +
                "  });" +
                "})();"
            );
        } catch (Exception ignored) {
            // Suppress prompt setup must never crash the test
        }
    }

        /** Registers the network response listener that captures order IDs. */
    private static void attachNetworkListener(Page page) {
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
            } catch (Exception ignored) {}
        });
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
