package com.tututu.robticket;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author: linchuyao
 * @date: 2019/9/30
 */
public class RobTicketWithDaMai {
    public static void main(String[] args) throws InterruptedException {
        System.setProperty("webdriver.chrome.driver","D:\\DevelopTools\\chromedriver_win32\\chromedriver.exe");
        String domainUrl ="https://www.damai.cn";
        //王力宏
//        String targetUrl = "https://detail.damai.cn/item.htm?spm=a2oeg.search_category.0.0.29c82244ecC2AH&id=601689680619&clicktitle=%E7%8E%8B%E5%8A%9B%E5%AE%8F%E2%80%9C%E9%BE%99%E7%9A%84%E4%BC%A0%E4%BA%BA2060%E2%80%9D%E4%B8%96%E7%95%8C%E5%B7%A1%E6%BC%94%E7%BB%8D%E5%85%B4%E7%AB%99";
        //林宥嘉
        String targetUrl = "https://detail.damai.cn/item.htm?spm=a2oeg.search_category.0.0.24951f41hmluwS&id=603729509189&clicktitle=%E6%9E%97%E5%AE%A5%E5%98%89idol%E4%B8%96%E7%95%8C%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%E2%80%94%E6%9D%AD%E5%B7%9E%E7%AB%99";


        Map<String, Object> contentSettings = new HashMap<String, Object>();
        contentSettings.put("images", 2);

        Map<String, Object> preferences = new HashMap<String, Object>();
        preferences.put("profile.default_content_settings", contentSettings);

        DesiredCapabilities caps = DesiredCapabilities.chrome();
        caps.setCapability("chrome.prefs", preferences);

        ChromeDriver chromeDriver = new ChromeDriver(caps);

        //大麦官网地址
        chromeDriver.navigate().to(domainUrl);

        //等待网页加载完
        while (!chromeDriver.getTitle().contains("大麦网-全球演出赛事官方购票平台")){
            Thread.sleep(1000);
        }

        //扫码登录
//        loginByScanQRCode(chromeDriver);

        //跳转到抢票页面
        chromeDriver.navigate().to(targetUrl);

        //抢票页轮训
        System.out.println("==============================================================");
        System.out.println("开始进行日期及票价选择");
        while (!chromeDriver.getTitle().contains("确认订单")){
            String btnText = chromeDriver.findElementByClassName("buybtn").getText();
            System.out.println("目前抢票页面状态:"+btnText);
            chromeDriver.navigate().to(targetUrl);
//            List<WebElement> list = chromeDriver.findElementByClassName("perform__order__select perform__order__select__performs").findElements(By.xpath("//div[2]/div[@class='select_right_list_item']"));
//            System.out.println(list);
        }
    }

    //扫二维码登录
    private static void loginByScanQRCode(ChromeDriver chromeDriver) throws InterruptedException {
        Thread.sleep(1000);
        WebElement loginBtnElement = null;
        try {
            loginBtnElement = chromeDriver.findElementByXPath("//div[@class='right-header']/div[1]/div[1]");
        }catch (NoSuchElementException e){
            System.out.println("未找到登录按钮");
            return;
        }
        if(loginBtnElement != null){
            loginBtnElement.click();
        }

        //打开二维码
        WebDriver frame = chromeDriver.switchTo().frame("alibaba-login-box");
        frame.findElement(By.id("login")).findElement(By.xpath("//div[1]/div[3]")).click();

        while (true) {
            try {
                Thread.sleep(1000);
                frame.findElement(By.id("login")).findElement(By.xpath("//div[1]/div[3]"));
            }catch (NoSuchElementException e){
                System.out.println("扫码登录成功!!!");
                break;
            }
        }
    }
}
