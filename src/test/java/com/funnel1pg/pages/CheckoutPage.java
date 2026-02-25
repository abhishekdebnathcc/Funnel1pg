package com.funnel1pg.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;

/**
 * CheckoutPage – handles the 1-page checkout form.
 *
 * PAYMENT FLOW (from live DOM inspection of 1pgCC25Feb):
 *   - All payment method tiles start hidden (d-none). JS reveals the ones configured
 *     in attr-providers on the <paymentmethod> element.
 *   - Credit card: click the tile to reveal #credit-card-fields, then fill card details.
 *   - COD: click the COD tile – no further fields needed.
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
        try {
            page.evaluate("document.querySelector('.sec-btn1').click()");
            page.waitForTimeout(500);
            System.out.println("✓ Product selected (JS click)");
        } catch (Exception e) {
            System.out.println("✗ Product select: " + e.getMessage());
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
                                    String city, String state, String zip,
                                    String email, String phone) {
        safeFill(INPUT_FIRST_NAME, firstName);
        safeFill(INPUT_LAST_NAME,  lastName);
        fillAddressField(address);
        safeSelectByLabel(SELECT_STATE, state);
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

    public void selectShippingMethod() {
        System.out.println("ℹ Shipping pre-selected – no action needed");
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    /**
     * Selects the Credit Card payment tile (revealing the CC fields), then fills them.
     * The CC fields section starts hidden (#credit-card-fields has d-none) until
     * the tile is clicked.
     */
    public void selectAndFillCreditCard(String cardNumber, String month, String year, String cvv) {
        clickPaymentTile(PM_CREDIT_TILE, "Credit Card");
        // Wait for #credit-card-fields to become visible
        try {
            page.waitForFunction(
                "!document.getElementById('credit-card-fields').classList.contains('d-none')",
                null,
                new Page.WaitForFunctionOptions().setTimeout(5000)
            );
        } catch (Exception ignored) {}
        safeFill(INPUT_CARD, cardNumber);
        safeSelectByLabel(SELECT_MONTH, month);
        safeSelectByLabel(SELECT_YEAR, year);
        safeFill(INPUT_CVV, cvv);
        System.out.println("✓ Credit card details filled");
    }

    /**
     * Selects the Cash on Delivery tile. No further payment details are needed.
     */
    public void selectCashOnDelivery() {
        clickPaymentTile(PM_COD_TILE, "Cash on Delivery");
        System.out.println("✓ Cash on Delivery selected");
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
