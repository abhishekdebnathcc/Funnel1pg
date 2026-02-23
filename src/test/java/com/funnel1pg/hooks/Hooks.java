package com.funnel1pg.hooks;

import com.funnel1pg.config.ConfigReader;
import com.funnel1pg.utils.PlaywrightManager;
import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Hooks {

    @Before
    public void setUp(Scenario scenario) {
        System.out.println("▶ " + scenario.getName());
        PlaywrightManager.initBrowser();
        try { Files.createDirectories(Paths.get(ConfigReader.getReportsDir() + "/screenshots")); }
        catch (Exception ignored) {}
    }

    @After
    public void tearDown(Scenario scenario) {
        if (scenario.isFailed() && ConfigReader.isScreenshotOnFailure()) {
            try {
                byte[] png = PlaywrightManager.getPage()
                        .screenshot(new Page.ScreenshotOptions().setFullPage(true));
                scenario.attach(png, "image/png", "Failure – " + scenario.getName());
            } catch (Exception e) {
                System.out.println("Screenshot failed: " + e.getMessage());
            }
        }
        System.out.println((scenario.isFailed() ? "❌ FAILED" : "✅ PASSED") + ": " + scenario.getName());
        PlaywrightManager.closeBrowser();
    }
}
