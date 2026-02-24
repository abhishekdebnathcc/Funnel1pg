package com.funnel1pg.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.SelectOption;

import java.util.List;

public class CheckoutPage extends BasePage {

    // ── Product ───────────────────────────────────────────────────────────────
    private static final String BTN_SELECT = "button:has-text('Select')";

    // ── Shipping Address (exact placeholders from live page) ──────────────────
    private static final String INPUT_FIRST_NAME = "input[placeholder='First Name']";
    private static final String INPUT_LAST_NAME  = "input[placeholder='Last Name']";
    private static final String INPUT_ADDRESS    = "input[placeholder='Enter a location']";
    // NOTE: :text-is() inside :has() is not valid — use nth-of-type or id instead
    private static final String SELECT_STATE     = "select#State, select[name='state'], select:nth-of-type(2)";
    private static final String INPUT_CITY       = "input[placeholder='City']";
    private static final String INPUT_ZIP        = "input[placeholder='Zip Code:']";
    private static final String INPUT_EMAIL      = "input[placeholder='Email address']";
    private static final String INPUT_PHONE      = "input[placeholder='Phone']";

    // ── Payment (exact placeholders from live page) ───────────────────────────
    private static final String INPUT_CARD   = "input[placeholder='Card Number']";
    private static final String INPUT_CVV    = "input[placeholder='Security code']";
    // Month/Year: first and second <select> without a placeholder option
    // They are the only two selects in the payment block
    private static final String SELECT_MONTH = "select#month, select[name='month']";
    private static final String SELECT_YEAR  = "select#year, select[name='year']";

    // ── Terms & Submit ────────────────────────────────────────────────────────
    private static final String BTN_BUY = "button:has-text('COMPLETE YOUR SECURE PURCHASE')";

    // ── Validation error selectors ────────────────────────────────────────────
    private static final String VALIDATION_ERRORS =
            ".error, .invalid, .field-error, [class*='error-msg'], [class*='validation']";

    private static final String PAYMENT_ERRORS =
            "[class*='card-error'], [class*='payment-error'], " +
            "[class*='decline'], [class*='error']";

    public CheckoutPage(Page page) { super(page); }

    // ── Product ───────────────────────────────────────────────────────────────

    public void selectFirstAvailableProduct() {
        try {
            waitForVisible(BTN_SELECT, 5_000);
            click(BTN_SELECT);
            System.out.println("✔ Product selected");
        } catch (Exception e) {
            System.out.println("ℹ No Select button — product may be pre-selected");
        }
    }

    // ── Shipping Address ──────────────────────────────────────────────────────

    public void fillShippingAddress(String firstName, String lastName, String address,
                                    String city, String state, String zip,
                                    String email, String phone) {
        safeFill(INPUT_FIRST_NAME, firstName);
        safeFill(INPUT_LAST_NAME,  lastName);
        fillAddressField(address);
        safeSelectByLabel(state);   // state dropdown
        safeFill(INPUT_CITY,  city);
        safeFill(INPUT_ZIP,   zip);
        safeFill(INPUT_EMAIL, email);
        safeFill(INPUT_PHONE, phone);
    }

    private void fillAddressField(String address) {
        try {
            page.locator(INPUT_ADDRESS).fill(address);
            page.waitForTimeout(700);
            page.keyboard().press("Escape");
        } catch (Exception e) {
            System.out.println("⚠ Address field: " + e.getMessage());
        }
    }

    // ── State Dropdown ────────────────────────────────────────────────────────

    private void safeSelectByLabel(String label) {
        // Try every <select> on the page and pick the one that has the matching option
        try {
            List<Locator> selects = page.locator("select").all();
            for (Locator select : selects) {
                try {
                    // Check if this select has an option matching the label
                    int count = select.locator("option:has-text('" + label + "')").count();
                    if (count > 0) {
                        select.selectOption(new SelectOption().setLabel(label));
                        System.out.println("✔ Selected [" + label + "] in dropdown");
                        return;
                    }
                } catch (Exception ignored) {}
            }
            System.out.println("⚠ Could not find dropdown with option: " + label);
        } catch (Exception e) {
            System.out.println("⚠ safeSelectByLabel [" + label + "]: " + e.getMessage());
        }
    }

    // ── Shipping Method ───────────────────────────────────────────────────────

    public void selectShippingMethod() {
        System.out.println("ℹ Vande Shipping pre-selected — no action needed");
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    public void fillPaymentDetails(String cardNumber, String month, String year, String cvv) {
        safeFill(INPUT_CARD, cardNumber);
        safeSelectMonthYear(month, year);
        safeFill(INPUT_CVV, cvv);
    }

    private void safeSelectMonthYear(String month, String year) {
        // Month and year are <select> elements that contain "Month"/"Year" as first option
        try {
            List<Locator> allSelects = page.locator("select").all();
            boolean monthDone = false;
            boolean yearDone  = false;
            for (Locator sel : allSelects) {
                try {
                    // Check first option text to identify month or year select
                    String firstOpt = sel.locator("option").first().textContent().trim();
                    if (!monthDone && firstOpt.equalsIgnoreCase("Month")) {
                        sel.selectOption(new SelectOption().setLabel(month));
                        System.out.println("✔ Month selected: " + month);
                        monthDone = true;
                    } else if (!yearDone && firstOpt.equalsIgnoreCase("Year")) {
                        sel.selectOption(new SelectOption().setLabel(year));
                        System.out.println("✔ Year selected: " + year);
                        yearDone = true;
                    }
                    if (monthDone && yearDone) break;
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("⚠ safeSelectMonthYear: " + e.getMessage());
        }
    }

    // ── Terms ─────────────────────────────────────────────────────────────────

    public void acceptTermsAndConditions() {
        try {
            List<Locator> boxes = page.locator("input[type='checkbox']").all();
            for (Locator cb : boxes) {
                try {
                    String parentText = cb.evaluate(
                            "el => el.closest('label,div,p')?.innerText || ''").toString();
                    if (parentText.toLowerCase().contains("terms")) {
                        if (!cb.isChecked()) cb.click();
                        System.out.println("✔ Terms checkbox checked");
                        return;
                    }
                } catch (Exception ignored) {}
            }
            // Fallback: last unchecked checkbox
            for (int i = boxes.size() - 1; i >= 0; i--) {
                try {
                    if (!boxes.get(i).isChecked()) {
                        boxes.get(i).click();
                        System.out.println("✔ Terms checked via fallback");
                        return;
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.out.println("⚠ Terms checkbox: " + e.getMessage());
        }
    }

    // ── Submit ────────────────────────────────────────────────────────────────

    public void clickCompletePurchase() {
        waitForVisible(BTN_BUY, 10_000);
        click(BTN_BUY);
        System.out.println("✔ Complete Purchase clicked");
    }

    // ── Validation Checks ─────────────────────────────────────────────────────

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

    // ── Private Helpers ───────────────────────────────────────────────────────

    private void safeFill(String selector, String value) {
        try {
            page.locator(selector).first().fill(value);
        } catch (Exception e) {
            System.out.println("⚠ safeFill [" + selector + "]: " + e.getMessage());
        }
    }
}
