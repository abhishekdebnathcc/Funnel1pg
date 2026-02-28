package com.funnel1pg.pages;

import com.funnel1pg.config.ConfigReader;
import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;

/**
 * CheckoutPage – handles the 1-page checkout form.
 *
 * PAYMENT FLOW:
 *   Tiles in #payment-methods-wrapper start hidden (d-none).
 *   JS removes d-none from configured tiles at runtime.
 *
 *   Case A – Single payment method (only CC):
 *     All tiles stay d-none. #credit-card-fields is already visible.
 *     -> Fill CC fields directly, no tile click needed.
 *
 *   Case B – Multiple payment methods:
 *     At least one tile is visible (d-none removed by JS).
 *     -> Click the CC tile to reveal #credit-card-fields, then fill.
 *
 * CROSS-SELL (1pgCC25Feb):
 *   - <crosssellproduct> elements render as .sel-crossprod.sel-prod product boxes.
 *   - Each has an "Add to Order" button: a.btn-select-product inside the box.
 */
public class CheckoutPage extends BasePage {

    // ── Product ───────────────────────────────────────────────────────────────
    private static final String BTN_SELECT_PRODUCT  = ".sec-btn1";

    // ── Cross-sell ────────────────────────────────────────────────────────────
    // crosssellproduct > div > .sel-crossprod > ... > a.btn-select-product
    private static final String CROSS_SELL_CONTAINER = "crosssellproduct";
    private static final String CROSS_SELL_ADD_BTN   = "a.btn-select-product";

    // ── Shipping Address ──────────────────────────────────────────────────────
    private static final String INPUT_FIRST_NAME = "#inputFirstName[name='firstName']";
    private static final String INPUT_LAST_NAME  = "#inputLastName[name='lastName']";
    private static final String INPUT_ADDRESS    = "#inputAddress";
    private static final String SELECT_COUNTRY   = "#shippingCountry";
    private static final String SELECT_STATE     = "#shippingState";
    private static final String INPUT_CITY       = "#inputCity";
    private static final String INPUT_ZIP        = "#fields_zip";
    private static final String INPUT_EMAIL      = "#inputEmail";
    private static final String INPUT_PHONE      = "#inputPhone";

    // ── Payment method tiles ──────────────────────────────────────────────────
    // All live under #payment-methods-wrapper; JS removes d-none on configured ones.
    private static final String PM_WRAPPER       = "#payment-methods-wrapper";
    private static final String PM_CREDIT_TILE   = "[data-payment='creditcard'] .payment-method-text";
    private static final String PM_COD_TILE      = "[data-payment='cash-on-delivery'] .payment-method-text";
    private static final String CC_FIELDS_DIV    = "#credit-card-fields";

    // ── Credit-card fields ────────────────────────────────────────────────────
    private static final String INPUT_CARD       = "#ccNumber";
    private static final String SELECT_MONTH     = "#fields_expmonth";
    private static final String SELECT_YEAR      = "#fields_expyear";
    private static final String INPUT_CVV        = "#cvv";

    // ── Terms & Submit ────────────────────────────────────────────────────────
    private static final String CB_TERMS = "#terms-conditions";
    private static final String BTN_BUY  = "button.checkout-form-submit";

    // ── Error selectors ───────────────────────────────────────────────────────
    private static final String VALIDATION_ERRORS =
            ".error, .invalid, .field-error, [class*='error-msg'], " +
            "[class*='validation-error'], input:invalid, select:invalid";
    private static final String PAYMENT_ERRORS =
            "[class*='card-error'], [class*='payment-error'], [class*='decline'], " +
            ".alert-danger, .alert-error";

    public CheckoutPage(Page page) { super(page); }

    // ── Product ───────────────────────────────────────────────────────────────

    public void selectFirstAvailableProduct() {
        if (ConfigReader.isSelectAllProducts()) {
            selectAllProducts();
            return;
        }
        try {
            page.evaluate("document.querySelector('.sec-btn1').click()");
            page.waitForTimeout(500);
            System.out.println("✓ Product selected (JS click)");
        } catch (Exception e) {
            System.out.println("✗ Product select: " + e.getMessage());
        }
    }

    /**
     * Selects ALL available main products (every .sec-btn1).
     * Activated when -Dselect.all.products=true.
     */
    public void selectAllProducts() {
        try {
            var btns = page.locator(BTN_SELECT_PRODUCT).all();
            System.out.println("i Selecting all " + btns.size() + " product(s)");
            for (var btn : btns) {
                try { btn.click(); page.waitForTimeout(300); } catch (Exception ignored) {}
            }
            System.out.println("✓ All products selected");
        } catch (Exception e) {
            System.out.println("✗ selectAllProducts: " + e.getMessage());
        }
    }

    // ── Cross-sell ────────────────────────────────────────────────────────────

    /**
     * Selects all visible cross-sell products by clicking their "Add to Order" button.
     * Skips any that are disabled (.cross-disabled).
     * Returns the count of cross-sells added.
     */
    public int selectAllCrossSellProducts() {
        int added = 0;
        try {
            // Wait briefly for JS to render cross-sell products
            page.waitForTimeout(1000);
            var crossSells = page.locator(CROSS_SELL_CONTAINER).all();
            if (crossSells.isEmpty()) {
                System.out.println("ℹ No cross-sell products found");
                return 0;
            }
            System.out.println("✓ Found " + crossSells.size() + " cross-sell product(s)");
            for (Locator cs : crossSells) {
                try {
                    // Skip if disabled
                    String cls = cs.locator(".sel-crossprod").getAttribute("class");
                    if (cls != null && cls.contains("cross-disabled")) {
                        System.out.println("  ⊘ Cross-sell is disabled – skipping");
                        continue;
                    }
                    String name = "";
                    try { name = cs.locator("h3").textContent().trim(); } catch (Exception ignored) {}
                    cs.locator(CROSS_SELL_ADD_BTN).click();
                    page.waitForTimeout(400);
                    added++;
                    System.out.println("  ✓ Cross-sell added: " + (name.isEmpty() ? "(unnamed)" : name));
                } catch (Exception e) {
                    System.out.println("  ✗ Could not add cross-sell: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("✗ Cross-sell selection error: " + e.getMessage());
        }
        return added;
    }

    // ── Shipping ──────────────────────────────────────────────────────────────

    public void fillShippingAddress(String firstName, String lastName, String address,
                                    String city, String zip,
                                    String email, String phone) {
        safeFill(INPUT_FIRST_NAME, firstName);
        safeFill(INPUT_LAST_NAME,  lastName);
        fillAddressField(address);
        // Pick country + state randomly from the live dropdown
        selectRandomCountryAndState();
        safeFill(INPUT_CITY,  city);
        safeFill(INPUT_ZIP,   zip);
        safeFill(INPUT_EMAIL, email);
        safeFill(INPUT_PHONE, phone);
        System.out.println("✓ Shipping address filled");
    }

    private void fillAddressField(String address) {
        try {
            page.locator(INPUT_ADDRESS).fill(address);
            page.waitForTimeout(800);
            page.keyboard().press("Escape");
            page.waitForTimeout(300);
        } catch (Exception e) {
            System.out.println("✗ Address field: " + e.getMessage());
        }
    }

    /**
     * Returns names of main products from <product attr-name="..."> custom elements.
     * Called immediately after selection while still on the checkout page.
     */
    public java.util.List<String> getSelectedMainProductNames() {
        var names = new java.util.ArrayList<String>();
        try {
            var products = page.locator("product").all();
            for (var p : products) {
                try {
                    String name = p.getAttribute("attr-name");
                    if (name != null && !name.trim().isEmpty()) {
                        String sale  = p.getAttribute("attr-sale-price");
                        String price = p.getAttribute("attr-price");
                        String eff   = (sale != null && !sale.isEmpty()
                                        && Double.parseDouble(sale) > 0) ? sale : price;
                        names.add(name.trim() + (eff != null && !eff.isEmpty() ? "  $" + eff : ""));
                        continue;
                    }
                } catch (Exception ignored) {}
                // Fallback: title inside the rendered .sec-btn1
                try {
                    String name = p.locator(".title-block__main").textContent().trim();
                    names.add(name.isEmpty() ? "Main Product" : name);
                } catch (Exception ignored) { names.add("Main Product"); }
            }
        } catch (Exception ignored) {}
        if (names.isEmpty()) names.add("Main Product");
        return names;
    }

    /**
     * Returns names of selected cross-sell products.
     * Reads attr-name from <crosssellproduct> elements whose btn text is no longer "Add to Order".
     * Must be called while still on the checkout page.
     */
    public java.util.List<String> getSelectedCrossSellNames() {
        var names = new java.util.ArrayList<String>();
        try {
            var crossSells = page.locator(CROSS_SELL_CONTAINER).all();
            for (var cs : crossSells) {
                try {
                    String btnText = cs.locator(CROSS_SELL_ADD_BTN).textContent().trim();
                    if (btnText.toLowerCase().contains("add to order")) continue; // not selected
                    String name = cs.getAttribute("attr-name");
                    if (name == null || name.trim().isEmpty())
                        name = cs.locator("h3, .product-title, .title-block__main").first().textContent().trim();
                    if (name == null || name.trim().isEmpty()) name = "Cross-sell Product";
                    String sale  = cs.getAttribute("attr-sale-price");
                    String price = cs.getAttribute("attr-price");
                    String eff   = (sale != null && !sale.isEmpty()
                                    && Double.parseDouble(sale) > 0) ? sale : price;
                    names.add(name.trim() + (eff != null && !eff.isEmpty() ? "  $" + eff : ""));
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return names;
    }

    public void selectShippingMethod() {
        System.out.println("ℹ Shipping pre-selected – no action needed");
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    /**
     * Smart credit card fill with single/multi payment method detection.
     *
     * Case A – Single payment method (credit card only):
     *   All tiles remain d-none. #credit-card-fields is already visible.
     *   -> Fill directly, no tile click needed.
     *
     * Case B – Multiple payment methods configured:
     *   At least one tile has d-none removed by JS (tile is visible).
     *   -> Click the credit card tile first to reveal #credit-card-fields, then fill.
     */
    public void selectAndFillCreditCard(String cardNumber, String month, String year, String cvv) {
        // Brief pause for JS to finish rendering payment method tiles
        page.waitForTimeout(800);

        if (hasVisiblePaymentTiles()) {
            System.out.println("i Multiple payment methods detected - clicking credit card tile");
            clickPaymentTile(PM_CREDIT_TILE, "Credit Card");
            // Wait for CC fields to become visible after tile click
            try {
                page.waitForFunction(
                    "!document.getElementById('credit-card-fields').classList.contains('d-none')",
                    null,
                    new Page.WaitForFunctionOptions().setTimeout(5000)
                );
            } catch (Exception ignored) {}
        } else {
            System.out.println("i Single payment method - CC fields already visible, filling directly");
        }

        safeFill(INPUT_CARD, cardNumber);
        safeSelectByLabel(SELECT_MONTH, month);
        safeSelectByLabel(SELECT_YEAR, year);
        safeFill(INPUT_CVV, cvv);
        System.out.println("✓ Credit card details filled");
    }

    /**
     * Selects the Cash on Delivery tile. Only clicks if multiple payment methods
     * are configured (tile is visible). If single method, CoD is already active.
     */
    public void selectCashOnDelivery() {
        page.waitForTimeout(800);
        if (hasVisiblePaymentTiles()) {
            clickPaymentTile(PM_COD_TILE, "Cash on Delivery");
        } else {
            System.out.println("i Single payment method configured - no CoD tile to click");
        }
        System.out.println("✓ Cash on Delivery selected");
    }

    /**
     * Returns true if at least one [data-payment] tile inside
     * #payment-methods-wrapper does NOT have the d-none class.
     * If all tiles are d-none the funnel has only one payment method
     * and shows the CC fields directly without a tile chooser.
     */
    private boolean hasVisiblePaymentTiles() {
        try {
            long visibleCount = Long.parseLong(page.evaluate(
                "(function() {" +
                "  var tiles = document.querySelectorAll('#payment-methods-wrapper [data-payment]');" +
                "  var n = 0;" +
                "  tiles.forEach(function(t) { if (!t.classList.contains('d-none')) n++; });" +
                "  return n;" +
                "})()"
            ).toString());
            System.out.println("i Visible payment tiles: " + visibleCount);
            return visibleCount > 0;
        } catch (Exception e) {
            System.out.println("! Could not detect payment tiles: " + e.getMessage());
            return false; // safe default: treat as single method, fill directly
        }
    }

    /**
     * Clicks a payment method tile. Waits for JS to reveal it (remove d-none),
     * then clicks via Playwright locator (not JS eval, to avoid selector escaping issues).
     */
    private void clickPaymentTile(String selector, String label) {
        try {
            // Wait up to 8s for JS to remove d-none from the payment method container
            page.waitForTimeout(1500); // JS needs a moment to run attr-providers logic
            // Try Playwright click (works even if element is inside a formerly-hidden container)
            Locator tile = page.locator(selector);
            tile.waitFor(new Locator.WaitForOptions()
                    .setState(com.microsoft.playwright.options.WaitForSelectorState.VISIBLE)
                    .setTimeout(8000));
            tile.click();
            page.waitForTimeout(500);
            System.out.println("✓ Payment tile clicked: " + label);
        } catch (Exception e) {
            // Fallback: scroll into view and force click
            System.out.println("⚠ Normal click failed for [" + label + "], trying force click");
            try {
                page.locator(selector).click(new Locator.ClickOptions().setForce(true));
                page.waitForTimeout(500);
                System.out.println("✓ Payment tile force-clicked: " + label);
            } catch (Exception ex) {
                System.out.println("✗ Could not click payment tile [" + label + "]: " + ex.getMessage());
            }
        }
    }

    /**
     * Returns a list of payment types that are currently visible (not d-none) on the page.
     * E.g. ["creditcard", "cash-on-delivery"]
     */
    public java.util.List<String> getVisiblePaymentMethods() {
        var methods = new java.util.ArrayList<String>();
        try {
            var tiles = page.locator("[data-payment]").all();
            for (Locator t : tiles) {
                try {
                    String cls = t.getAttribute("class");
                    String type = t.getAttribute("data-payment");
                    if (cls != null && !cls.contains("d-none") && type != null) {
                        methods.add(type);
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
        return methods;
    }

    // ── Legacy compatibility (used by step defs for invalid card test) ─────────

    public void fillPaymentDetails(String cardType, String cardNumber,
                                   String month, String year, String cvv) {
        selectAndFillCreditCard(cardNumber, month, year, cvv);
    }

    // ── Terms & Submit ────────────────────────────────────────────────────────

    public void acceptTermsAndConditions() {
        try {
            Locator cb = page.locator(CB_TERMS);
            if (!cb.isChecked()) {
                cb.click();
                System.out.println("✓ Terms checkbox checked");
            } else {
                System.out.println("ℹ Terms already checked");
            }
        } catch (Exception e) {
            System.out.println("✗ Terms checkbox: " + e.getMessage());
        }
    }

    public void clickCompletePurchase() {
        waitForVisible(BTN_BUY, 10_000);
        click(BTN_BUY);
        System.out.println("✓ Complete Purchase clicked");
    }

    // ── Validation ────────────────────────────────────────────────────────────

    public boolean hasValidationErrors() {
        try {
            if (page.locator("input:invalid, select:invalid").count() > 0) return true;
            return page.locator(VALIDATION_ERRORS).count() > 0;
        } catch (Exception e) { return false; }
    }

    public boolean hasPaymentError() {
        try {
            return page.locator(PAYMENT_ERRORS).count() > 0;
        } catch (Exception e) { return false; }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private void safeFill(String selector, String value) {
        try {
            page.locator(selector).fill(value);
        } catch (Exception e) {
            System.out.println("✗ safeFill [" + selector + "]: " + e.getMessage());
        }
    }


    /**
     * Reads live country dropdown options, picks one at random, selects it,
     * waits for the state dropdown to populate, then picks a random state.
     * Returns [countryCode, stateCode] so the caller can log them.
     */
    public String[] selectRandomCountryAndState() {
        try {
            // Collect all valid country options (skip blank placeholder)
            java.util.List<String> countryCodes = new java.util.ArrayList<>();
            for (var opt : page.locator(SELECT_COUNTRY + " option").all()) {
                String val = opt.getAttribute("value");
                if (val != null && !val.trim().isEmpty()) countryCodes.add(val.trim());
            }
            if (countryCodes.isEmpty()) {
                System.out.println("✗ No country options found – defaulting to US");
                return new String[]{"US", "CA"};
            }
            String countryCode = countryCodes.get(new java.util.Random().nextInt(countryCodes.size()));
            safeSelectByValue(SELECT_COUNTRY, countryCode);
            page.waitForTimeout(600);

            // Wait for state dropdown to populate
            java.util.List<String> stateCodes = new java.util.ArrayList<>();
            long deadline = System.currentTimeMillis() + 5000;
            while (System.currentTimeMillis() < deadline) {
                for (var opt : page.locator(SELECT_STATE + " option").all()) {
                    String val = opt.getAttribute("value");
                    if (val != null && !val.trim().isEmpty()) stateCodes.add(val.trim());
                }
                if (!stateCodes.isEmpty()) break;
                page.waitForTimeout(300);
                stateCodes.clear();
            }

            String stateCode = stateCodes.isEmpty() ? "" :
                stateCodes.get(new java.util.Random().nextInt(stateCodes.size()));
            if (!stateCode.isEmpty()) safeSelectByValue(SELECT_STATE, stateCode);

            System.out.println("i Country: " + countryCode + "  State: " + stateCode);
            return new String[]{countryCode, stateCode};
        } catch (Exception e) {
            System.out.println("✗ selectRandomCountryAndState: " + e.getMessage());
            return new String[]{"US", "CA"};
        }
    }
    private void safeSelectByValue(String selector, String value) {
        try {
            page.locator(selector).selectOption(new SelectOption().setValue(value));
        } catch (Exception e) {
            System.out.println("✗ safeSelectByValue [" + selector + "]='" + value + "': " + e.getMessage());
        }
    }

    private void safeSelectByLabel(String selector, String label) {
        try {
            page.locator(selector).selectOption(new SelectOption().setLabel(label));
        } catch (Exception e) {
            try {
                page.locator(selector).selectOption(label);
            } catch (Exception ex) {
                System.out.println("✗ safeSelect [" + selector + "]='" + label + "': " + ex.getMessage());
            }
        }
    }
}
