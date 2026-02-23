package com.funnel1pg.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;

public class TestDataReader {

    private static final JsonNode checkoutData;

    static {
        try (InputStream is = TestDataReader.class.getClassLoader()
                .getResourceAsStream("testdata/checkout_data.json")) {
            checkoutData = new ObjectMapper().readTree(is);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load checkout_data.json", e);
        }
    }

    public static String getCustomer(String field) {
        return checkoutData.path("customer").path(field).asText();
    }

    public static String getPayment(String field) {
        return checkoutData.path("payment").path(field).asText();
    }
}
