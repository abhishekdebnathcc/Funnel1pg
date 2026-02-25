package com.funnel1pg.utils;

import com.funnel1pg.config.ConfigReader;
import java.io.File;
import java.nio.file.Paths;

/**
 * Opens the Extent Report in the user's browser after test execution.
 *
 * Key design: we open Chrome in background (non-blocking) and do NOT
 * wait for the process to exit. On macOS, `open -a "Google Chrome"` opens
 * a new tab in the existing Chrome window and returns immediately — Chrome
 * itself stays running so the tab is never closed.
 */
public class ReportOpener {

    public static void openReportInChrome() {
        try {
            String reportPath = Paths.get(ConfigReader.getReportsDir(), "extent-report.html")
                    .toAbsolutePath().toString();

            File reportFile = new File(reportPath);
            if (!reportFile.exists()) {
                System.out.println("⚠ Report file not found: " + reportPath);
                return;
            }

            String fileUrl = "file://" + reportPath;
            String os = System.getProperty("os.name", "").toLowerCase();

            if (os.contains("mac")) {
                openMac(fileUrl);
            } else if (os.contains("win")) {
                openWindows(fileUrl);
            } else {
                openLinux(fileUrl);
            }

        } catch (Exception e) {
            System.out.println("⚠ Could not open report: " + e.getMessage());
        }
    }

    private static void openMac(String fileUrl) {
        try {
            // `-g` → open without bringing Chrome to the foreground (keeps existing focus)
            // We do NOT call waitFor() – Chrome stays open because we're not the process keeping it alive.
            new ProcessBuilder("open", "-a", "Google Chrome", fileUrl)
                    .inheritIO()
                    .start();
            System.out.println("✅ Report opened in Chrome");
        } catch (Exception e) {
            try {
                // Fallback: use the default browser
                new ProcessBuilder("open", fileUrl).inheritIO().start();
                System.out.println("✅ Report opened in default browser");
            } catch (Exception ex) {
                System.out.println("⚠ Could not open report browser: " + ex.getMessage());
            }
        }
    }

    private static void openWindows(String fileUrl) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "chrome", fileUrl)
                    .inheritIO().start();
            System.out.println("✅ Report opened in Chrome");
        } catch (Exception e) {
            try {
                new ProcessBuilder("cmd", "/c", "start", fileUrl).inheritIO().start();
                System.out.println("✅ Report opened in default browser");
            } catch (Exception ex) {
                System.out.println("⚠ Could not open report: " + ex.getMessage());
            }
        }
    }

    private static void openLinux(String fileUrl) {
        try {
            new ProcessBuilder("google-chrome", fileUrl).inheritIO().start();
            System.out.println("✅ Report opened in Chrome");
        } catch (Exception e) {
            try {
                new ProcessBuilder("xdg-open", fileUrl).inheritIO().start();
                System.out.println("✅ Report opened in default browser");
            } catch (Exception ex) {
                System.out.println("⚠ Could not open report: " + ex.getMessage());
            }
        }
    }
}
