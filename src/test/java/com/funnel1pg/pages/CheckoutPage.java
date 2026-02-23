package com.funnel1pg.pages;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;

public class CheckoutPage extends BasePage {

    public CheckoutPage(Page page) { super(page); }

    public void selectFirstAvailableProduct() {
        try {
            page.locator("button:has-text('Select')").first().click();
            System.out.println("Product selected via Select button");
        } catch (Exception e) {
            System.out.println("No Select button found — product may be pre-selected");
        }
    }

    public void fillShippingAddress(String firstName, String lastName, String address,
                                    String city, String state, String zip,
                                    String email, String phone) {
        safeFill("input[placeholder='First Name']",          firstName);
        safeFill("input[placeholder='Last Name']",           lastName);
        // Address with autocomplete dismiss
        try {
            page.locator("input[placeholder*='location']").first().fill(address);
            page.waitForTimeout(600);
            page.keyboard().press("Escape");
        } catch (Exception e) {
            safeField("address", address);
        }
        safeField("city",  city);
        // State dropdown
        try {
            page.locator("select").filter(new Locator.FilterOptions().setHasText("Select State"))
                    .first().selectOption(state);
        } catch (Exception e) {
            System.out.println("State select fallback: " + e.getMessage());
        }
        safeFillPlaceholder("Zip",   zip);
        safeType("email", email);
        safeType("tel",   phone);
    }

    private void safeField(String name, String value) {
        try { page.locator("input[placeholder*='" + name + "']").first().fill(value); }
        catch (Exception e) { System.out.println("safeField miss: " + name); }
    }

    private void safeFillPlaceholder(String placeholder, String value) {
        try { page.locator("input[placeholder*='" + placeholder + "']").first().fill(value); }
        catch (Exception e) { System.out.println("safeFilPlaceholder miss: " + placeholder); }
    }

    private void safeType(String type, String value) {
        try { page.locator("input[type='" + type + "']").first().fill(value); }
        catch (Exception e) { System.out.println("safeType miss: " + type); }
    }

    private void safeFill(String selector, String value) {
        try { page.locator(selector).first().fill(value); }
        catch (Exception e) { System.out.println("safeF miss: " + selector); }
    }

    public void selectShippingMethod() {
        try { page.locator("input[type='radio']").first().click(); }
        catch (Exception e) { System.out.println("Shipping already selected or not clickable"); }
    }

    public void fillPaymentDetails(String cardNumber, String month, String year, String cvv) {
        safeFill("input[placeholder='Card Number']",    cardNumber);
        try {
            page.locator("select").filter(new Locator.FilterOptions().setHasText("Month"))
                    .first().selectOption(month);
            page.locator("select").filter(new Locator.FilterOptions().setHasText("Year"))
                    .first().selectOption(year);
        } catch (Exception e) {
            System.out.println("Month/Year select: " + e.getMessage());
        }
        safeFill("input[placeholder='Security code']", cvv);
    }

    public void acceptTermsAndConditions() {
        try {
            Locator cb = page.locator("input[type='checkbox']").filter(
                    new Locator.FilterOptions().setHasText("terms"));
            if (!cb.isChecked()) cb.click();
        } catch (Exception e1) {
            // fallback: click last unchecked checkbox
            try {
                page.locator("input[type='checkbox']").last().click();
            } catch (Exception e2) {
                System.out.println("Terms checkbox not found");
            }
        }
    }

    public void clickCompletePurchase() {
        page.locator("button:has-text('COMPLETE YOUR SECURE PURCHASE')").click();
    }
}
