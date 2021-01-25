package com.epam.selenium;

import io.github.bonigarcia.wdm.*;
import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.*;
import org.openqa.selenium.devtools.*;

import java.time.*;

public class SeleniumTests {

    private static WebDriver driver;
    private static DevTools chromeDevTools;
    private static String appUrl;


    @BeforeAll
    public static void before() {
        WebDriverManager.chromedriver().setup();
        appUrl = "http://localhost:8080";
        driver = new ChromeDriver();
        chromeDevTools = ((ChromeDriver) driver).getDevTools();
        chromeDevTools.createSession();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));

    }


    @Test
    public void findOwnerTest() {
        driver.get(appUrl);
        driver.findElement(By.xpath("//a[@title='find owners']")).click();
        driver.findElement(By.id("lastName")).sendKeys("Davis");
        driver.findElement(By.xpath("//button[@type='submit']")).click();
        String owner = driver.findElement(By.xpath("//a[@href='/owners/2']")).getText();
        Assertions.assertEquals("Betty Davis", owner);
    }

    @Test
    public void vetListTest() {
        driver.get(appUrl);
        driver.findElement(By.xpath("//a[@title='veterinarians']")).click();
        String vet = driver.findElement(By.xpath("//td")).getText();
        Assertions.assertEquals("James Carter", vet);
    }


}
