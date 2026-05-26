package com.expandtesting.notes.utils;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;

/**
 * PerformanceUtils — Direct W3C Navigation Timing API collection utility.
 * Satisfies Section 3.5 non-functional requirements for UI performance tracking.
 */
public class PerformanceUtils {
    private static final Logger log = LogManager.getLogger(PerformanceUtils.class);

    /**
     * Extracts precise page-load milestones directly from the browser window object.
     */
    public static void collectUiPerformanceMetrics(WebDriver driver, String testCaseId) {
        try {
            JavascriptExecutor js = (JavascriptExecutor) driver;

            // Extract millisecond milestones from the browser's native timing layer
            long navigationStart = (Long) js.executeScript("return window.performance.timing.navigationStart;");
            long domInteractive  = (Long) js.executeScript("return window.performance.timing.domInteractive;");
            long loadEventEnd    = (Long) js.executeScript("return window.performance.timing.loadEventEnd;");

            // Calculate core performance engineering benchmarks
            long domReadyTimeMs = domInteractive - navigationStart;
            long totalPageLoadTimeMs = loadEventEnd - navigationStart;

            log.info("====================================================================================");
            log.info("⏱️ UI PERFORMANCE BENCHMARK REPORT — TEST CASE: {}", testCaseId);
            log.info("• DOM Interactive Readiness Timing : {} ms", domReadyTimeMs);
            log.info("• Full Page Load/Asset Completion   : {} ms", totalPageLoadTimeMs);
            log.info("====================================================================================");

        } catch (Exception e) {
            log.warn("Performance Tracker Notice: Could not extract browser metrics for this run: {}", e.getMessage());
        }
    }
}