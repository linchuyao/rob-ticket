package com.tututu.robticket;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author: linchuyao
 * @date: 2019/9/30
 */
public class RobTicketWithDaMai {

    //cookie存储地址
    private static final String COOKIE_PATH = "src/main/resources/cookies.pkl";

    //chromDriver驱动地址-mac系统
    private static final String CHROME_DRIVER_PATH_MAC = "";

    //chromDriver驱动地址-windows系统
    private static final String CHROME_DRIVER_PATH_WINDOWS = "";

    //大麦首页
    private static final String DOMAIN_URL = "https://www.damai.cn";

    //蔡依林
    private static final String TARGET_URL = "https://detail.damai.cn/item.htm?id=611259138626";

    //选择场次
    private static final int[] SESSION_SET = {2};

    //票位
    private static final int[] TICKET_GRADEN_SET = {3};

    //是否加载照片(不加载可提高网速)
    private static final boolean UNLOAD_IMAGE = false;

    private static final ExecutorService ROB_TICKET_THREAD_POOL = new ThreadPoolExecutor(10,20,5, TimeUnit.MINUTES, new LinkedBlockingDeque<>(), new ThreadFactoryBuilder().build(),new ThreadPoolExecutor.AbortPolicy());


    public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
        for (int i = 0; i < 10; i++) {
            ROB_TICKET_THREAD_POOL.submit(()->{
                try {
                    robTicket();
                } catch (Exception e) {
                    System.out.println(e);
                }
            });
        }

        //阻塞线程
        while (true){}
    }

    /**
     * 抢票
     *
     * @throws InterruptedException
     * @throws IOException
     * @throws ClassNotFoundException
     */
    private static void robTicket() throws InterruptedException, IOException, ClassNotFoundException {

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
        if(existsCookies()){
            setCookies(chromeDriver);
        }else {
            loginByScanQRCode(chromeDriver);
            saveCookies(chromeDriver);
        }

        //跳转到抢票页面
        chromeDriver.navigate().to(TARGET_URL);

        //抢票页轮训
        while (!chromeDriver.getTitle().contains("确认订单")) {

            String btnText = chromeDriver.findElementByClassName("buybtn").getText();
            System.out.println("====================目前抢票页面状态: " + btnText + "================================");

            //选择场次和价位
            checkSessionAndTicketGraden(chromeDriver);

            //判断页面状态
            if ("立即预订".equals(btnText) || "立即购买".equals(btnText)) {
                //购票人+1并且提交
                addTicketNum(chromeDriver);
                Thread.sleep(50);
                //创建订单
                chromeDriver.findElementByClassName("buybtn").click();

            }
            if ("即将开抢".equals(btnText) || "即将开售".equals(btnText) || "提交缺货登记".equals(btnText)) {
                chromeDriver.navigate().to(TARGET_URL);
            }

        }

        Thread.sleep(200);

        //选择购票人
        chooseBuyer(chromeDriver);

        //提交订单
//        orderSubmit(chromeDriver);
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
    private static void chooseBuyer(ChromeDriver chromeDriver) {
        WebElement buyerWebElement = chromeDriver.findElementByClassName("buyer-list");
        buyerWebElement.findElements(By.className("buyer-list-item")).forEach(t -> {
            t.click();
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 获取浏览器驱动
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

    private static boolean existsCookies(){
        return new File(COOKIE_PATH).exists();
    }

    /**
     * 保存cookies到本地
     *
     * @param chromeDriver
     * @throws IOException
     */
    private static void saveCookies(ChromeDriver chromeDriver) throws IOException {
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
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private static void setCookies(ChromeDriver chromeDriver) throws IOException, ClassNotFoundException {
        ObjectInputStream fos = new ObjectInputStream(new FileInputStream(RobTicketWithDaMai.COOKIE_PATH));
        Set<Cookie> cookies = (Set<Cookie>) fos.readObject();
        cookies.forEach(chromeDriver.manage()::addCookie);
    }

    /**
     * 选择场次和票位
     *
     * @param chromeDriver*/
    private static void checkSessionAndTicketGraden(ChromeDriver chromeDriver) {

        System.out.println("==========================开始进行日期及票价选择====================================");

        //进入选场次和选票环节
        List<WebElement> performOrderSelects = chromeDriver
            .findElementsByClassName("perform__order__select");

        //获取场次
        WebElement sessionWebElement = performOrderSelects.stream()
            .filter(t -> "场次".equals(t.findElement(By.className("select_left")).getText()))
            .findFirst().get();

        List<WebElement> sessionWebElements = sessionWebElement.findElements(By
            .className("select_right_list_item"));

        //选择场次
        for (int i = 0; i < RobTicketWithDaMai.SESSION_SET.length; i++) {
            WebElement webElement = sessionWebElements.get(RobTicketWithDaMai.SESSION_SET[i] - 1);
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

        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        performOrderSelects = chromeDriver
                .findElementsByClassName("perform__order__select");

//        获取票价
        WebElement ticketGradeWebElement = performOrderSelects.stream()
                .filter(t -> "票档".equals(t.findElement(By.className("select_left")).getText()))
                .findFirst().get();


        List<WebElement> ticketGradeWebElements = ticketGradeWebElement.findElements(By
                .className("select_right_list_item"));


        //选择票价
        for (int i = 0; i < TICKET_GRADEN_SET.length; i++) {
            WebElement webElement = ticketGradeWebElements.get(TICKET_GRADEN_SET[i] - 1);
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
                break;
            }
        }
    }
}
