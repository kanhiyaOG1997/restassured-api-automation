package com.kanhaiya.api.listeners;

import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

/**
 * Re-runs a failed test once. The public API sometimes resets or responds slowly,
 * so a single retry avoids false failures. A real bug still fails twice and is reported.
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final int MAX_RETRIES = 1;   // how many extra attempts a failed test gets
    private int attempts = 0;                   // TestNG creates one analyzer per test method

    @Override
    public boolean retry(ITestResult result) {
        if (attempts < MAX_RETRIES) {
            attempts++;
            System.out.println("Retrying " + result.getName() + " (attempt " + (attempts + 1) + ")");
            return true;                        // true = run the test again
        }
        return false;                           // give up, report the failure
    }
}
