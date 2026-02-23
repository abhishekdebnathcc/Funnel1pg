package com.funnel1pg.stepdefs;

import com.funnel1pg.config.ConfigReader;
import com.funnel1pg.pages.CheckoutPage;
import com.funnel1pg.pages.ThankYouPage;
import com.funnel1pg.pages.UpsellPage;
import com.funnel1pg.utils.PlaywrightManager;
import com.funnel1pg.utils.TestDataReader;
import com.microsoft.playwright.Page;
import io.cucumber.java.en.*;

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

    @Given("I navigate to the checkout page")
    public void navigateToCheckout() {
        init();
        page.navigate(ConfigReader.getBaseUrl());
        page.waitForLoadState();
        System.out.println("URL: " + page.url());
    }

    @When("I select a product on the main page")
    public void selectProduct() {
        checkoutPage.selectFirstAvailableProduct();
    }

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
    }

    @When("I select a shipping method")
    public void selectShipping() { checkoutPage.selectShippingMethod(); }

    @When("I fill in the payment details with test card")
    public void fillPayment() {
        checkoutPage.fillPaymentDetails(
                TestDataReader.getPayment("cardNumber"),
                TestDataReader.getPayment("expiryMonth"),
                TestDataReader.getPayment("expiryYear"),
                TestDataReader.getPayment("cvv")
        );
    }

    @When("I accept the terms and conditions")
    public void acceptTerms() { checkoutPage.acceptTermsAndConditions(); }

    @When("I click the complete purchase button")
    public void clickPurchase() {
        checkoutPage.clickCompletePurchase();
        page.waitForLoadState();
        page.waitForTimeout(3000);
        System.out.println("Post-purchase URL: " + page.url());
    }

    @Then("I should be taken to an upsell page or thank you page")
    public void verifyPostPurchasePage() {
        boolean advanced = upsellPage.isUpsellPage() || thankYouPage.isThankYouPageDisplayed()
                || !page.url().equals(ConfigReader.getBaseUrl());
        assertTrue(advanced, "Expected upsell or thank-you page, got: " + page.url());
    }

    @And("I navigate through any upsell pages")
    public void navigateUpsells() {
        if (!thankYouPage.isThankYouPageDisplayed()) {
            upsellPage.navigateThroughAllUpsells();
        }
    }

    @Then("I should land on the thank you page")
    public void verifyThankYouPage() {
        page.waitForLoadState();
        System.out.println("Final URL: " + page.url());
        System.out.println("Heading:   " + thankYouPage.getHeading());
        assertTrue(thankYouPage.isThankYouPageDisplayed(),
                "Expected Thank You page but got: " + page.url());
        System.out.println("✅ Order complete — Thank You page confirmed!");
    }
}
