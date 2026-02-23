package com.funnel1pg.hooks;

import com.funnel1pg.config.ConfigReader;
import com.funnel1pg.utils.PlaywrightManager;
import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.Scenario;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Hooks {

    @Before(order = 1)
    public void setUp(Scenario scenario) {
        System.out.println("\n══════════════════════════════════════════");
        System.out.println("▶ SCENARIO : " + scenario.getName());
        System.out.println("  TAGS     : " + scenario.getSourceTagNames());
        System.out.println("══════════════════════════════════════════");

        PlaywrightManager.initBrowser();

        // Ensure screenshots dir exists
        try {
            Path screenshotsDir = Paths.get(ConfigReader.getReportsDir(), "screenshots");
            Files.createDirectories(screenshotsDir);
        } catch (Exception e) {
            System.out.println("⚠ Could not create screenshots dir: " + e.getMessage());
        }
    }

    @After(order = 1)
    public void tearDown(Scenario scenario) {
        // Capture screenshot on failure and embed in Cucumber report
        if (scenario.isFailed() && ConfigReader.isScreenshotOnFailure()) {
            try {
                byte[] png = PlaywrightManager.getPage()
                        .screenshot(new Page.ScreenshotOptions().setFullPage(true));
                scenario.attach(png, "image/png",
                        "Screenshot on failure: " + scenario.getName());
                System.out.println("📸 Failure screenshot captured");
            } catch (Exception e) {
                System.out.println("⚠ Screenshot failed: " + e.getMessage());
            }
        }

        String icon = scenario.isFailed() ? "❌ FAILED" : "✅ PASSED";
        System.out.println(icon + " : " + scenario.getName() + "\n");

        PlaywrightManager.closeBrowser();
    }
}
