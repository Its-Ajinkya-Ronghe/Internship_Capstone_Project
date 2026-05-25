package listeners;

import base.BaseTest;
import io.qameta.allure.Attachment;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        Object testClass = result.getInstance();
        // Extract the unique ThreadLocal driver instance running inside the failed scenario class context
        WebDriver driver = ((BaseTest) testClass).getDriver();

        if (driver != null) {
            saveScreenshot(result.getName(), driver);
        }
    }

    // Allure attachment wrapper capturing failure layouts dynamically
    @Attachment(value = "Failure Screenshot - {testName}", type = "image/png")
    public byte[] saveScreenshot(String testName, WebDriver driver) {
        return ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
    }

    @Override
    public void onStart(ITestContext context) {
        System.out.println("Beginning Automation Cycle Verification Suite: " + context.getName());
    }

    @Override
    public void onFinish(ITestContext context) {
        System.out.println("Execution Suite Run Completed.");
    }
}