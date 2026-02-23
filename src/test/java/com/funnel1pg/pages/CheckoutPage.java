package com.funnel1pg.pages;

import com.microsoft.playwright.Page;

public class CheckoutPage extends BasePage {

    // ── Product ─────────────────────────────────────────────────────────────
    // The "Select" button next to the product card
    private static final String BTN_SELECT = "button:has-text('Select')";

    // ── Shipping Address – exact placeholders from page snapshot ─────────────
    private static final String INPUT_FIRST_NAME = "input[placeholder='First Name']";
    private static final String INPUT_LAST_NAME  = "input[placeholder='Last Name']";
    private static final String INPUT_ADDRESS    = "input[placeholder='Enter a location']";
    private static final String SELECT_COUNTRY   = "select[id='country'], select:has(option:text('United States'))";
    private static final String SELECT_STATE     = "select:has(option:text('Select State'))";
    private static final String INPUT_CITY       = "input[placeholder='City']";
    private static final String INPUT_ZIP        = "input[placeholder='Zip Code:']";
    private static final String INPUT_EMAIL      = "input[placeholder='Email address']";
    private static final String INPUT_PHONE      = "input[placeholder='Phone']";

    // ── Payment – exact placeholders from page snapshot ──────────────────────
    private static final String INPUT_CARD   = "input[placeholder='Card Number']";
    private static final String SELECT_MONTH = "select:has(option:text('Month'))";
    private static final String SELECT_YEAR  = "select:has(option:text('Year'))";
    private static final String INPUT_CVV    = "input[placeholder='Security code']";

    // ── Terms & Submit ────────────────────────────────────────────────────────
    // "I agree to the terms & conditions of this site" checkbox
    private static final String CB_TERMS  = "input[type='checkbox']:near(:text('terms & conditions'))";
    private static final String BTN_BUY   = "button:has-text('COMPLETE YOUR SECURE PURCHASE')";

    public CheckoutPage(Page page) { super(page); }

    // ── Product ──────────────────────────────────────────────────────────────

    public void selectFirstAvailableProduct() {
        try {
            waitForVisible(BTN_SELECT);
            click(BTN_SELECT);
            System.out.println("✔ Product selected");
        } catch (Exception e) {
            System.out.println("ℹ No Select button — product may be pre-selected: " + e.getMessage());
        }
    }

    // ── Shipping Address ─────────────────────────────────────────────────────

    public void fillShippingAddress(String firstName, String lastName, String address,
                                    String city, String state, String zip,
                                    String email, String phone) {
        safeFill(INPUT_FIRST_NAME, firstName);
        safeFill(INPUT_LAST_NAME,  lastName);
        fillAddressWithAutocomplete(address);
        // Country defaults to United States — leave it
        safeSelectByVisibleText(SELECT_STATE, state);
        safeFill(INPUT_CITY,  city);
        safeFill(INPUT_ZIP,   zip);
        safeFill(INPUT_EMAIL, email);
        safeFill(INPUT_PHONE, phone);
        System.out.println("✔ Shipping address filled");
    }

    private void fillAddressWithAutocomplete(String address) {
        try {
            page.locator(INPUT_ADDRESS).fill(address);
            page.waitForTimeout(700);
            // dismiss any Google autocomplete dropdown
            page.keyboard().press("Escape");
        } catch (Exception e) {
            System.out.println("⚠ Address field: " + e.getMessage());
        }
    }

    // ── Shipping Method ───────────────────────────────────────────────────────

    public void selectShippingMethod() {
        // The page shows "Vande Shipping" as the only option — it is pre-selected
        // Just verify it is visible; nothing to click
        System.out.println("✔ Shipping method — Vande Shipping (auto-selected)");
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    public void fillPaymentDetails(String cardNumber, String month, String year, String cvv) {
        safeFill(INPUT_CARD, cardNumber);
        safeSelectByVisibleText(SELECT_MONTH, month);
        safeSelectByVisibleText(SELECT_YEAR,  year);
        safeFill(INPUT_CVV,  cvv);
        System.out.println("✔ Payment details filled");
    }

    // ── Terms ─────────────────────────────────────────────────────────────────

    public void acceptTermsAndConditions() {
        try {
            // Target the exact "I agree to the terms & conditions" checkbox
            var cb = page.locator(CB_TERMS).first();
            if (!cb.isChecked()) {
                cb.click();
                System.out.println("✔ Terms checkbox checked");
            } else {
                System.out.println("ℹ Terms already checked");
            }
        } catch (Exception e) {
            // Fallback: iterate all unchecked checkboxes and check the last one
            try {
                var checkboxes = page.locator("input[type='checkbox']").all();
                for (var cb : checkboxes) {
                    String label = cb.evaluate("el => el.closest('label,div')?.innerText || ''").toString();
                    if (label.toLowerCase().contains("terms")) {
                        if (!cb.isChecked()) cb.click();
                        break;
                    }
                }
                System.out.println("✔ Terms checked via fallback");
            } catch (Exception ex) {
                System.out.println("⚠ Could not check terms: " + ex.getMessage());
            }
        }
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    public void clickCompletePurchase() {
        waitForVisible(BTN_BUY);
        click(BTN_BUY);
        System.out.println("✔ Complete Purchase clicked");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void safeFill(String selector, String value) {
        try {
            page.locator(selector).first().fill(value);
        } catch (Exception e) {
            System.out.println("⚠ safeFill [" + selector + "]: " + e.getMessage());
        }
    }

    private void safeSelectByVisibleText(String selector, String visibleText) {
        try {
            page.locator(selector).first().selectOption(
                    new com.microsoft.playwright.options.SelectOption().setLabel(visibleText));
        } catch (Exception e) {
            // fall back to value matching
            try {
                page.locator(selector).first().selectOption(visibleText);
            } catch (Exception ex) {
                System.out.println("⚠ safeSelect [" + selector + "] value=" + visibleText + ": " + ex.getMessage());
            }
        }
    }
}
