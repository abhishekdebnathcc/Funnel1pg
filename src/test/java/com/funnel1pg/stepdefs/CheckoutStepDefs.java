package com.funnel1pg.stepdefs;

import com.aventstack.extentreports.Status;
import com.funnel1pg.config.ConfigReader;
import com.funnel1pg.pages.CheckoutPage;
import com.funnel1pg.pages.ThankYouPage;
import com.funnel1pg.pages.UpsellPage;
import com.funnel1pg.utils.ExtentReportManager;
import com.funnel1pg.utils.PlaywrightManager;
import com.funnel1pg.utils.TestDataReader;
import com.microsoft.playwright.Page;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CheckoutStepDefs {

    private Page         page;
    private CheckoutPage checkoutPage;
    private UpsellPage   upsellPage;
    private ThankYouPage thankYouPage;

    private void init() {
        page         = PlaywrightManager.getPage();
        checkoutPage = new CheckoutPage(page);
        upsellPage   = new UpsellPage(page);
        thankYouPage = new ThankYouPage(page);
    }

    private void log(String msg) {
        System.out.println(msg);
        var t = ExtentReportManager.getTest();
        if (t != null) t.log(Status.INFO, msg);
    }

    // ── Navigation ────────────────────────────────────────────────────────────

    @Given("I navigate to the checkout page")
    public void navigateToCheckout() {
        init();
        page.navigate(ConfigReader.getBaseUrl());
        page.waitForLoadState();
        log("🌐 Loaded: " + page.url());
    }

    // ── Product ───────────────────────────────────────────────────────────────

    @When("I select a product on the main page")
    public void selectProduct() {
        checkoutPage.selectFirstAvailableProduct();
        log("✔ Product selected");
    }

    // ── Shipping ──────────────────────────────────────────────────────────────

    @When("I fill in the shipping address with valid details")
    public void fillShippingAddress() {
        checkoutPage.fillShippingAddress(
                TestDataReader.getCustomer("firstName"),
                TestDataReader.getCustomer("lastName"),
                TestDataReader.getCustomer("address"),
                TestDataReader.getCustomer("city"),
                TestDataReader.getCustomer("state"),
                TestDataReader.getCustomer("zipCode"),
                TestDataReader.getCustomer("email"),
                TestDataReader.getCustomer("phone")
        );
        log("✔ Shipping address filled");
    }

    @When("I select a shipping method")
    public void selectShipping() {
        checkoutPage.selectShippingMethod();
        log("✔ Shipping method: Vande Shipping (pre-selected)");
    }

    // ── Payment ───────────────────────────────────────────────────────────────

    @When("I fill in the payment details with test card")
    public void fillPayment() {
        checkoutPage.fillPaymentDetails(
                TestDataReader.getPayment("cardNumber"),
                TestDataReader.getPayment("expiryMonth"),
                TestDataReader.getPayment("expiryYear"),
                TestDataReader.getPayment("cvv")
        );
        log("✔ Payment filled (test card)");
    }

    @When("I fill in the payment details with invalid card")
    public void fillInvalidPayment() {
        checkoutPage.fillPaymentDetails(
                "1234567890123456",
                TestDataReader.getPayment("expiryMonth"),
                TestDataReader.getPayment("expiryYear"),
                "000"
        );
        log("✔ Payment filled (invalid card)");
    }

    // ── Terms & Submit ────────────────────────────────────────────────────────

    @When("I accept the terms and conditions")
    public void acceptTerms() {
        checkoutPage.acceptTermsAndConditions();
        log("✔ Terms accepted");
    }

    @When("I click the complete purchase button")
    public void clickPurchase() {
        checkoutPage.clickCompletePurchase();
        // Explicit Predicate<String> to avoid overload ambiguity
        Predicate<String> notCheckout = url -> !url.contains("checkout");
        try {
            page.waitForURL(notCheckout,
                    new Page.WaitForURLOptions().setTimeout(15_000));
        } catch (Exception e) {
            page.waitForLoadState();
            page.waitForTimeout(3000);
        }
        log("🔄 Post-purchase URL: " + page.url());
    }

    @When("I click the complete purchase button without filling any fields")
    public void clickPurchaseEmpty() {
        checkoutPage.clickCompletePurchase();
        page.waitForTimeout(1500);
        log("🔄 Submitted empty — checking validation errors");
    }

    // ── Assertions ────────────────────────────────────────────────────────────

    @Then("I should be taken to an upsell page or thank you page")
    public void verifyPostPurchasePage() {
        boolean advanced = upsellPage.isUpsellPage()
                || thankYouPage.isThankYouPageDisplayed()
                || !page.url().contains("checkout");
        log("📍 Post-purchase URL: " + page.url());
        assertTrue(advanced,
                "Expected upsell or thank-you page after purchase, got: " + page.url());
    }

    @And("I navigate through any upsell pages")
    public void navigateUpsells() {
        if (!thankYouPage.isThankYouPageDisplayed()) {
            upsellPage.navigateThroughAllUpsells();
        } else {
            log("ℹ Already on thank-you page — no upsells");
        }
    }

    @Then("I should land on the thank you page")
    public void verifyThankYouPage() {
        page.waitForLoadState();
        boolean onThankYou = thankYouPage.isThankYouPageDisplayed();
        log("📍 Final URL    : " + page.url());
        log("📄 Page heading : " + thankYouPage.getHeading());
        assertTrue(onThankYou,
                "Expected Thank You page but got: " + page.url());
        log("🎉 ORDER COMPLETE — Thank You page confirmed!");
    }

    @Then("I should see required field validation errors on the form")
    public void verifyValidationErrors() {
        boolean hasErrors = checkoutPage.hasValidationErrors();
        log("🔍 Validation errors present: " + hasErrors);
        assertTrue(hasErrors,
                "Expected form validation errors after empty submission");
    }

    @Then("I should see a payment error message")
    public void verifyPaymentError() {
        boolean hasError = checkoutPage.hasPaymentError();
        log("🔍 Payment error present: " + hasError);
        assertTrue(hasError,
                "Expected payment error for invalid card");
    }
}
