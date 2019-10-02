package com.tututu.robticket;

import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @author: linchuyao
 * @date: 2019/9/30
 */
public class RobTicketWithDaMai {
    private static final String COOKIE_PATH = "/Users/tututu/Desktop/cookies.pkl";
    private static final String CHROME_DRIVER_PATH_MAC = "/Users/tututu/Downloads/chromedriver";
    private static final String CHROME_DRIVER_PATH_WINDOWS = "D:\\DevelopTools\\chromedriver_win32\\chromedriver.exe";

    //大麦首页
    private static final String DOMAIN_URL = "https://www.damai.cn";

    //王力宏宁波
//    private static final String TARGET_URL = "https://detail.damai.cn/item.htm?id=604300702129";

    //王力宏绍兴
    private static final String TARGET_URL = "https://detail.damai.cn/item.htm?spm=a2oeg.search_category.0.0.29c82244zUwUg2&id=601689680619&clicktitle=%E7%8E%8B%E5%8A%9B%E5%AE%8F%E2%80%9C%E9%BE%99%E7%9A%84%E4%BC%A0%E4%BA%BA2060%E2%80%9D%E4%B8%96%E7%95%8C%E5%B7%A1%E6%BC%94%E7%BB%8D%E5%85%B4%E7%AB%99";
    //林宥嘉
//    private static final String TARGET_URL = "https://detail.damai.cn/item.htm?spm=a2oeg.search_category.0.0.24951f41hmluwS&id=603729509189&clicktitle=%E6%9E%97%E5%AE%A5%E5%98%89idol%E4%B8%96%E7%95%8C%E5%B7%A1%E5%9B%9E%E6%BC%94%E5%94%B1%E4%BC%9A%E2%80%94%E6%9D%AD%E5%B7%9E%E7%AB%99";

    //场次
    private static final int[] SESSIONSET = {1};

    //票位
    private static final int[] TICKETGRADENSET = {1};

    private static final boolean UNLOAD_IMAGE = true;


    public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {

        setChromeDriverPath();


        //静止加载图片启动
        ChromeDriver chromeDriver = getChromeDriver(UNLOAD_IMAGE);

        //大麦官网地址
        chromeDriver.navigate().to(DOMAIN_URL);

        //等待网页加载完
        while (!chromeDriver.getTitle().contains("大麦网-全球演出赛事官方购票平台")) {
            Thread.sleep(1000);
        }

        //存在cookies就设置到浏览器 不存在就扫码登陆
        if(existsCookies(COOKIE_PATH)){
            setCookies(chromeDriver,COOKIE_PATH);
        }else {
            loginByScanQRCode(chromeDriver);
        }

        //跳转到抢票页面
        chromeDriver.navigate().to(TARGET_URL);

        //抢票页轮训
        while (!chromeDriver.getTitle().contains("确认订单")) {

            String btnText = chromeDriver.findElementByClassName("buybtn").getText();
            System.out.println("====================目前抢票页面状态: " + btnText + "================================");

            //选择场次和价位
            checkSessionAndTicketGraden(SESSIONSET, TICKETGRADENSET, chromeDriver);

            //判断页面状态
            if ("立即预订".equals(btnText) || "立即购买".equals(btnText)) {
                //添加投票日并且提交
                addTicketNum(chromeDriver);
                //创建订单
                chromeDriver.findElementByClassName("buybtn").click();

            }
            if ("即将开抢".equals(btnText) || "即将开售".equals(btnText)) {
                chromeDriver.navigate().to(TARGET_URL);
            }

        }

        Thread.sleep(200);

        //选择购票人
        chooseByuer(chromeDriver);

        //提交订单
        orderSubmit(chromeDriver);

    }


    /**
     * 提交订单
     *
     * @param chromeDriver
     */
    private static void orderSubmit(ChromeDriver chromeDriver) {
        List<WebElement> buttonWebElements = chromeDriver.findElementsByTagName("button");
        buttonWebElements.forEach(t -> {
            if ("同意以上协议并提交订单".equals(t.getText())) {
                t.click();
            }
        });
    }

    /**
     * 选择购票人
     *
     * @param chromeDriver
     */
    private static void chooseByuer(ChromeDriver chromeDriver) {
        WebElement byuerWebElement = chromeDriver.findElementByClassName("buyer-list");
        byuerWebElement.findElements(By.className("buyer-list-item")).forEach(t -> {
            t.click();
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取浏览器渠道
     *
     * @param unLoadImage 是否不加载图片
     * @return
     */
    private static ChromeDriver getChromeDriver(boolean unLoadImage) {
        if(unLoadImage){
            Map<String, Object> preferences = new HashMap<String, Object>();
            preferences.put("profile.managed_default_content_settings.images", 2);
            ChromeOptions chromeOptions = new ChromeOptions();
            chromeOptions.setExperimentalOption("prefs", preferences);
            ChromeDriver chromeDriver = new ChromeDriver(chromeOptions);
            return chromeDriver;
        }else {
            return new ChromeDriver();
        }
    }


    /**
     * 设置chromeDriver路径
     */
    private static void setChromeDriverPath() {
        String osName = System.getProperty("os.name");
        if(osName.startsWith("Windows")){
            System.setProperty("webdriver.chrome.driver",CHROME_DRIVER_PATH_WINDOWS);
        }else if(osName.startsWith("Mac OS")){
            System.setProperty("webdriver.chrome.driver", CHROME_DRIVER_PATH_MAC);
        }
    }

    private static boolean existsCookies(String path){
        return new File(path).exists();
    }

    /**
     * 保存cookies到本地
     *
     * @param chromeDriver
     * @param path
     * @throws IOException
     */
    private static void saveCookies(ChromeDriver chromeDriver, String path) throws IOException {
        //写入cookie
        ObjectOutputStream fos = new ObjectOutputStream(new FileOutputStream(COOKIE_PATH));
        Set<Cookie> cookies = chromeDriver.manage().getCookies();
        fos.writeObject(cookies);
        fos.flush();
        fos.close();
    }

    /**
     * 设置cookies到浏览器
     *
     * @param chromeDriver
     * @param path
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void setCookies(ChromeDriver chromeDriver,String path) throws IOException, ClassNotFoundException {
        ObjectInputStream fos = new ObjectInputStream(new FileInputStream(path));
        Set<Cookie> cookies = (Set<Cookie>) fos.readObject();
        cookies.forEach(chromeDriver.manage()::addCookie);
    }

    /**
     * 选择场次和票位
     *
     * @param sessionSet
     * @param ticketGradenSet
     * @param chromeDriver
     */
    private static void checkSessionAndTicketGraden(int[] sessionSet, int[] ticketGradenSet, ChromeDriver chromeDriver) {

        System.out.println("==============================================================");
        System.out.println("开始进行日期及票价选择");

        //进入选场次和选票环节

        List<WebElement> performOrderSelects = chromeDriver
            .findElementsByClassName("perform__order__select");

        //获取场次
        WebElement sessionWebElement = performOrderSelects.stream()
            .filter(t -> "场次".equals(t.findElement(By.className("select_left")).getText()))
            .findFirst().get();

        //获取票价
        WebElement ticketGradeWebElement = performOrderSelects.stream()
            .filter(t -> "票档".equals(t.findElement(By.className("select_left")).getText()))
            .findFirst().get();

        List<WebElement> sessionWebElements = sessionWebElement.findElements(By
            .className("select_right_list_item"));
        List<WebElement> ticketGradeWebElements = ticketGradeWebElement.findElements(By
            .className("select_right_list_item"));

        //选择场次
        for (int i = 0; i < sessionSet.length; i++) {
            WebElement webElement = sessionWebElements.get(sessionSet[i] - 1);
            try {
                WebElement presellWebElement = webElement.findElement(By.className("presell"));
                if ("无票".equals(presellWebElement.getText())) {
                    continue;
                }

                if ("预售".equals(presellWebElement.getText())) {
                    webElement.click();
                }
            } catch (Exception e) {
                webElement.click();
            }
        }

        //选择票价
        for (int i = 0; i < ticketGradenSet.length; i++) {
            WebElement webElement = ticketGradeWebElements.get(ticketGradenSet[i] - 1);
            try {
                webElement.findElement(By.className("notticket"));
            } catch (Exception e) {
                webElement.click();
            }
        }
    }

    /**
     * 购票人加1
     *
     * @param chromeDriver
     * @throws InterruptedException
     */
    private static void addTicketNum(ChromeDriver chromeDriver) throws InterruptedException {
        boolean addHumanFlag = false;
        while (!addHumanFlag) {
            try {
                chromeDriver.findElementByXPath(
                    "//html//body//div[@class = 'perform__order__price']//div[2]//div//div//a[2]")
                    .click();
            } catch (Exception e) {
                Thread.sleep(100);
                continue;
            }
            addHumanFlag = true;
        }
    }

    /**
     * 扫二维码登录
     *
     * @param chromeDriver
     * @throws InterruptedException
     * @throws IOException
     */
    private static void loginByScanQRCode(ChromeDriver chromeDriver) throws InterruptedException, IOException {
        Thread.sleep(1000);
        WebElement loginBtnElement = null;
        try {
            loginBtnElement = chromeDriver
                .findElementByXPath("//div[@class='right-header']/div[1]/div[1]");
        } catch (NoSuchElementException e) {
            System.out.println("未找到登录按钮");
            return;
        }
        if (loginBtnElement != null) {
            loginBtnElement.click();
        }

        //打开二维码
        WebDriver frame = chromeDriver.switchTo().frame("alibaba-login-box");
        frame.findElement(By.id("login")).findElement(By.xpath("//div[1]/div[3]")).click();

        while (true) {
            try {
                Thread.sleep(1000);
                frame.findElement(By.id("login")).findElement(By.xpath("//div[1]/div[3]"));
            } catch (NoSuchElementException e) {
                System.out.println("扫码登录成功!!!");
                saveCookies(chromeDriver,COOKIE_PATH);
                break;
            }
        }
    }
}
