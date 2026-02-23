package com.funnel1pg.utils;

import com.funnel1pg.config.ConfigReader;
import com.microsoft.playwright.Page;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ScreenshotUtil {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static byte[] takeScreenshot(Page page, String label) {
        String ts   = LocalDateTime.now().format(FMT);
        String safe = label.replaceAll("[^a-zA-Z0-9]", "_");
        String path = ConfigReader.getReportsDir() + "/screenshots/" + safe + "_" + ts + ".png";
        return page.screenshot(new Page.ScreenshotOptions()
                .setPath(Paths.get(path))
                .setFullPage(true));
    }
}
