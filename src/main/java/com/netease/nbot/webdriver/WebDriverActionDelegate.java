package com.netease.nbot.webdriver;

import org.openqa.selenium.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;

@Component
public class WebDriverActionDelegate {

    @Value("${webdriver.screenShot.path}")
    private String PREFIX_FILE_PATH;

    private static final String OPEN_TAB_JAVASCRIPT = "var a = document.createElement(\"a\");\n" +
            "a.setAttribute(\"href\", \"https://www.baidu.com/\");\n" +
            "a.setAttribute(\"target\", \"_blank\");\n" +
            "a.setAttribute(\"id\", \"camnpr\");\n" +
            "document.body.appendChild(a);\n" +
            "a.click();";

    /**
     * @param driver
     * @param url
     * @param createWindow
     */
    public void to(WebDriver driver, String url, boolean createWindow) {
        try {
            driver.get(url);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param driver
     * @param url
     * @param createWindow
     */
    public void openWindow(WebDriver driver, String url, boolean createWindow) {
        try {
            driver.manage().window();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    /**
     * @param driver
     */
    public void reset(WebDriver driver) {
        Set<String> windowSet = driver.getWindowHandles();
        int size = windowSet.size();
        int i = 0;
        for (String windowTitle : windowSet) {
            driver.switchTo().window(windowTitle).manage().deleteAllCookies();
            driver.manage().getCookies();
            size--;
            if (size > 1) {
                driver.switchTo().window(windowTitle).close();
            } else {
                driver.get("https://www.baidu.com");
            }
        }
    }

    /**
     * @param driver
     * @param by
     * @param text
     */
    public void textInput(WebDriver driver, By by, String text, boolean append) {
        WebElement element = driver.findElement(by);

        if (!append) {
            element.clear();
        }
        element.sendKeys(text);
    }

    /**
     * @param driver
     * @param by
     */
    public void click(WebDriver driver, By by) {
        driver.findElement(by).click();
    }

    /**
     * @param driver
     * @param javaScript
     * @param args
     */
    public void runJavaScript(WebDriver driver, String javaScript, Object... args) {
        ((JavascriptExecutor) driver).executeScript(javaScript, args);
    }

    /**
     * @param driver
     * @param imageElement
     * @param imageType
     * @return
     */
    public File takeScreenShot(WebDriver driver, WebElement imageElement, String imageType) {
        try {
            File screen = ((TakesScreenshot) driver)
                    .getScreenshotAs(FILE);

            Point p = imageElement.getLocation();
            int width = imageElement.getSize().getWidth();
            int height = imageElement.getSize().getHeight();
            java.awt.Rectangle rect = new java.awt.Rectangle(width, height);
            BufferedImage img = null;
            img = ImageIO.read(screen);
            BufferedImage dest = img.getSubimage(p.getX(), p.getY(), rect.width,
                    rect.height);
            ImageIO.write(dest, imageType, screen);
            return screen;
        } catch (WebDriverException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public File takeScreenShotInFrame(WebDriver driver, WebElement imageElement, String imageType,  Point frameLocation) {
        try {
            File screen = ((TakesScreenshot) driver)
                    .getScreenshotAs(FILE);

            Point p = imageElement.getLocation();
            int width = imageElement.getSize().getWidth();
            int height = imageElement.getSize().getHeight();
            java.awt.Rectangle rect = new java.awt.Rectangle(width, height);
            BufferedImage img = null;
            img = ImageIO.read(screen);
            BufferedImage dest = img.getSubimage(p.getX() + frameLocation.getX(), p.getY() + frameLocation.getY(), rect.width,
                    rect.height);
            ImageIO.write(dest, imageType, screen);
            return screen;
        } catch (WebDriverException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param driver
     * @return
     */
    public File takeFullScreenShot(WebDriver driver) {
        try {
            File screen = ((TakesScreenshot) driver)
                    .getScreenshotAs(FILE);
            return screen;
        } catch (WebDriverException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param driver
     * @param element
     * @return
     */
    public boolean isElementPresent(WebDriver driver, WebElement element) {

        return false;
    }

    /**
     * @param driver
     * @return
     */
    public boolean isAlertPresent(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException Ex) {
            return false;
        }
    }

    /**
     * @param driver
     */
    public void acceptAlert(WebDriver driver) {
        Alert alert = driver.switchTo().alert();
        alert.accept();
    }

    /**
     * @param driver
     * @return
     */
    public String getAlertText(WebDriver driver) {
        Alert alert = driver.switchTo().alert();
        return alert.getText();
    }

    /**
     * @param driver
     * @return
     */
    public boolean isConfirmPresent(WebDriver driver) {
        try {
            driver.switchTo().alert();
            return true;
        } catch (NoAlertPresentException Ex) {
            return false;
        }
    }

    /**
     * @param driver
     */
    public void acceptConfirm(WebDriver driver) {
        Alert alert = driver.switchTo().alert();
        alert.accept();
    }

    /**
     * @param driver
     */
    public void cancleConfirm(WebDriver driver) {
        Alert alert = driver.switchTo().alert();
        alert.dismiss();
    }

    /**
     * @param driver
     * @return
     */
    public String getConfirmText(WebDriver driver) {
        Alert alert = driver.switchTo().alert();
        return alert.getText();
    }

    /**
     * @param driver
     */
    public void switchToLastWindow(WebDriver driver) {
        Object[] handles = driver.getWindowHandles()
                .toArray();
        int id = handles.length - 1;
        driver.switchTo().window(handles[id].toString());
    }

    public void switchFrame(WebDriver driver, String xpath) {
        driver.switchTo().frame(driver.findElement(By.xpath(xpath)));
    }

    /**
     * @param driver
     * @param by
     * @return
     */
    public boolean isElementDisplayed(WebDriver driver, By by) {
        try {
            return driver.findElement(by).isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    public String openTab(WebDriver driver) {
        Set<String> windowSet = driver.getWindowHandles();
        runJavaScript(driver, OPEN_TAB_JAVASCRIPT);
        Set<String> newWindowSet = driver.getWindowHandles();
        for (String window : newWindowSet) {
            if (!windowSet.contains(window)) {
                driver.switchTo().window(window);
                return window;
            }
        }
        return driver.getWindowHandle();
    }


    public void closeTab(WebDriver driver, String name) {
        if (name != null) {
            driver.switchTo().window(name).close();
            Set<String> windowSet = driver.getWindowHandles();
            if (!windowSet.isEmpty()) {
                driver.switchTo().window(new ArrayList<String>(windowSet).get(0));
            }
        } else {
            driver.close();
        }
    }

    /**
     * @param driver
     * @param by
     * @param content2Wait
     * @param RUN_COUNT
     * @return
     */
    public boolean isElementDisplayed(WebDriver driver, By by, String content2Wait, int RUN_COUNT) {
        boolean isLoad = false;
        for (int i = 0; i <= RUN_COUNT; i++) {
            isLoad = isElementDisplayed(driver, by);
            try {
                if (!(isLoad && driver.findElement(by).getText().contains(content2Wait))) {
                    Thread.sleep(1000);
                } else {
                    break;
                }
            } catch (InterruptedException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return isLoad;
    }

    /**
     * @param driver
     */
    public static void back(WebDriver driver) {
        driver.navigate().back();
    }

    /**
     * @param driver
     */
    public static void forword(WebDriver driver) {
        driver.navigate().forward();
    }

    /**
     * @param driver
     * @param cookie
     */
    public void addCookie(WebDriver driver, Cookie cookie) {
        driver.manage().addCookie(cookie);
    }


    OutputType<File> FILE = new OutputType<File>() {
        public File convertFromBase64Png(String base64Png) {
            return save(BYTES.convertFromBase64Png(base64Png));
        }

        public File convertFromPngBytes(byte[] data) {
            return save(data);
        }

        private File save(byte[] data) {
            OutputStream stream = null;
            String FILE_PATH = PREFIX_FILE_PATH + "/";
            File tmpPath = new File(FILE_PATH);
            if (!tmpPath.exists()) {
                tmpPath.mkdirs();
            }
            try {
                File tmpFile = File.createTempFile("screenshot", ".png", tmpPath);
                tmpFile.deleteOnExit();

                stream = new FileOutputStream(tmpFile);
                stream.write(data);

                return tmpFile;
            } catch (IOException e) {
                throw new WebDriverException(e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // Nothing sane to do
                    }
                }
            }
        }

        public String toString() {
            return "OutputType.FILE";
        }
    };
}
