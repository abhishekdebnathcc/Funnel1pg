@smoke @checkout
Feature: Funnel Checkout Flow - Smoke Test

  As a customer
  I want to place an order through the funnel checkout
  So that I can complete my purchase including any upsell pages

  Background:
    Given I navigate to the checkout page

  @order
  Scenario: Complete full funnel order flow from checkout to thank you page
    When I select a product on the main page
    And I fill in the shipping address with valid details
    And I select a shipping method
    And I fill in the payment details with test card
    And I accept the terms and conditions
    And I click the complete purchase button
    Then I should be taken to an upsell page or thank you page
    And I navigate through any upsell pages
    Then I should land on the thank you page
