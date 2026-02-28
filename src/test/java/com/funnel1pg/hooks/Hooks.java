package com.funnel1pg.hooks;

import com.aventstack.extentreports.Status;
import com.funnel1pg.config.ConfigReader;
import com.funnel1pg.utils.ExtentReportManager;
import com.funnel1pg.utils.PlaywrightManager;
import com.funnel1pg.utils.ReportOpener;
import com.microsoft.playwright.Page;
import io.cucumber.java.After;
import io.cucumber.java.AfterAll;
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

        // Always launch a brand-new browser instance for each scenario
        PlaywrightManager.initBrowser();

        ExtentReportManager.createTest(
                scenario.getName(),
                "Tags: " + scenario.getSourceTagNames() + " | URI: " + scenario.getUri());

        try {
            Path dir = Paths.get(ConfigReader.getReportsDir(), "screenshots");
            Files.createDirectories(dir);
        } catch (Exception e) {
            System.out.println("⚠ Could not create screenshots dir: " + e.getMessage());
        }
    }

    @After(order = 1)
    public void tearDown(Scenario scenario) {
        var extentTest = ExtentReportManager.getTest();

        // ── Screenshot on failure ─────────────────────────────────────────────
        if (scenario.isFailed()) {
            if (ConfigReader.isScreenshotOnFailure()) {
                try {
                    byte[] png = PlaywrightManager.getPage()
                            .screenshot(new Page.ScreenshotOptions().setFullPage(true));

                    scenario.attach(png, "image/png", "Failure screenshot: " + scenario.getName());

                    String safeName = scenario.getName().replaceAll("[^a-zA-Z0-9]", "_");
                    String filePath = ConfigReader.getReportsDir()
                            + "/screenshots/" + safeName + "_FAILED.png";
                    Files.write(Paths.get(filePath), png);

                    if (extentTest != null) {
                        extentTest.addScreenCaptureFromPath(
                                "screenshots/" + safeName + "_FAILED.png", "Failure Screenshot");
                    }
                    System.out.println("📸 Screenshot: " + filePath);
                } catch (Exception e) {
                    System.out.println("⚠ Screenshot failed: " + e.getMessage());
                }
            }
            if (extentTest != null) extentTest.log(Status.FAIL, "Scenario FAILED: " + scenario.getName());
        } else {
            if (extentTest != null) extentTest.log(Status.PASS, "Scenario PASSED");
        }

        System.out.println((scenario.isFailed() ? "❌ FAILED" : "✅ PASSED")
                + " : " + scenario.getName() + "\n");

        // Always close the browser after every scenario so Playwright
        // resources are fully released before the report flushes.
        PlaywrightManager.closeBrowser();
        System.out.println("🔴 Browser closed");
    }

    @AfterAll
    public static void afterAll() {
        ExtentReportManager.flushReports();
        System.out.println("📊 Extent report saved → reports/extent-report.html");

        System.out.println("\n🌐 Opening test report in Chrome...");
        ReportOpener.openReportInChrome();
    }
}
