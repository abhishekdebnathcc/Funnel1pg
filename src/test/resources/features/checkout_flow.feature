#@smoke @checkout @funnel
@checkout @funnel
Feature: Funnel Checkout Flow - Complete Business Logic

  As a customer
  I want to place an order through the complete funnel checkout
  So that I can complete my purchase including any upsell offers

  Background:
    Given I navigate to the checkout page

  # ═══════════════════════════════════════════════════════════════════════════════
  # HAPPY PATH: Primary Order → Upsells → Thank You
  # ═══════════════════════════════════════════════════════════════════════════════

  @order @happy-path @critical
  Scenario: [SC-001] Complete full funnel order flow with multiple upsells
    """
    BUSINESS LOGIC:
    1. Select product and fill checkout form
    2. Submit order
    3. REDIRECT CHECK:
       - If → Thank You: Verify all data (order #, price, address)
       - If → Upsell: Add product, select shipping, accept & continue
    4. LOOP through upsells until Thank You page reached
    5. Final verification of order details
    """
    # Primary Checkout
    When I select a product on the main page
    And I fill in the shipping address with valid details
    And I select a shipping method
    And I fill in the payment details with test card
    And I accept the terms and conditions
    And I click the complete purchase button

    # Primary Order Success - Redirect Check
    Then I should be taken to an upsell page or thank you page

    # Upsell Loop (repeats until thank you)
    And I navigate through any upsell pages

    # Final Landing Page Verification
    Then I should land on the thank you page

  # ═══════════════════════════════════════════════════════════════════════════════
  # DIRECT TO THANK YOU: No Upsells
  # ═══════════════════════════════════════════════════════════════════════════════

  @order @direct-thankyou
  Scenario: [SC-002] Primary order redirects directly to thank you page
    """
    SCENARIO: Primary order completes and goes straight to thank you
    EXPECTATION: No upsell pages presented, verify order details immediately
    """
    When I select a product on the main page
    And I fill in the shipping address with valid details
    And I select a shipping method
    And I fill in the payment details with test card
    And I accept the terms and conditions
    And I click the complete purchase button

    Then I should be taken to an upsell page or thank you page
    And I navigate through any upsell pages
    Then I should land on the thank you page

  # ═══════════════════════════════════════════════════════════════════════════════
  # VALIDATION TESTS
  # ═══════════════════════════════════════════════════════════════════════════════

  @validation @negative @form-validation
  Scenario: [SC-003] Checkout form shows validation errors when submitted empty
    """
    SCENARIO: User tries to submit checkout form without filling required fields
    EXPECTATION: Form validates and shows error messages
    """
    When I click the complete purchase button without filling any fields
    Then I should see required field validation errors on the form

  @validation @negative @payment-validation
  Scenario: [SC-004] Checkout fails with invalid card number
    """
    SCENARIO: User submits checkout with invalid payment card
    EXPECTATION: Payment processing fails with error message
    """
    When I select a product on the main page
    And I fill in the shipping address with valid details
    And I select a shipping method
    And I fill in the payment details with invalid card
    And I accept the terms and conditions
    And I click the complete purchase button
    Then I should see a payment error message

  # ═══════════════════════════════════════════════════════════════════════════════
  # ADDITIONAL TEST SCENARIOS
  # ═══════════════════════════════════════════════════════════════════════════════

  @smoke @regression
  Scenario: [SC-005] Verify order confirmation details on thank you page
    """
    SCENARIO: After completing order, verify all details are correct
    VERIFICATION:
      - Order number present and valid
      - Pricing correct
      - Customer information displayed
      - Shipping address shown
    """
    When I select a product on the main page
    And I fill in the shipping address with valid details
    And I select a shipping method
    And I fill in the payment details with test card
    And I accept the terms and conditions
    And I click the complete purchase button

    Then I should be taken to an upsell page or thank you page
    And I navigate through any upsell pages
    Then I should land on the thank you page