package com.kevin.edge_solar;

import ch.qos.logback.classic.util.ContextInitializer;
import com.kevin.edge_solar.service.SolarTestData;
import org.apache.commons.codec.binary.Base64;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class EdgeSolarApplication {
    private static final Logger logger = LoggerFactory.getLogger(EdgeSolarApplication.class);
    private final SolarTestData solarTestData;

    public EdgeSolarApplication(SolarTestData solarTestData) {
        this.solarTestData = solarTestData;
    }

    public static void main(String[] args) {
        System.setProperty(ContextInitializer.CONFIG_FILE_PROPERTY, "/resources/logback.xml");
        SolarTestData solarTestData = new SolarTestData();
        EdgeSolarApplication edgeSolarApplication = new EdgeSolarApplication(solarTestData);

        int sleepSec = 300;

        // 주기적인 작업을 위한
        final ScheduledThreadPoolExecutor exec = new ScheduledThreadPoolExecutor(1);
        exec.scheduleAtFixedRate(() -> {
            try {
                edgeSolarApplication.run();
            } catch (Exception e) {
                logger.error("MainClass 스케줄링 작업 도중에 오류가 발생했습니다. JAVA 오류 코드 : {}", e.getMessage());
                // 에러 발생시 Executor를 중지시킨다
                exec.shutdown() ;
            }
        }, 0, sleepSec, TimeUnit.SECONDS);
    }

    public void run() {
        Map<String, Object> solarMap;
        Map<String ,Object> interfaceMap;
        solarMap = solarTestData.createSolar();
        Map<String, Object> testDataMap = new HashMap<>();

        try {
            if(!solarMap.isEmpty()) {
                testDataMap.put("solar", solarMap);
            }
            final SimpleDateFormat fmt = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            Calendar cal = Calendar.getInstance(Locale.KOREA);
            String date = fmt.format(cal.getTime());

            logger.info("############## TEST DATA(SOLAR) ###############");
            logger.info("DATE : {}", date);
            logger.info("############# SEND TEST DATA EDGE #############");
            logger.info("ID : {}, TOTAL_WH : {}", ((Map<?, ?>)solarMap.get("9996_1")).get("sensor_sn"), ((Map<?, ?>)solarMap.get("9996_1")).get("total_wh"));
            logger.info("TESTDATA : {}", solarMap.get("9996_1"));

            interfaceMap = getProperties();
            String ip = interfaceMap.get("edge_ip") + ":8080";

            JSONObject jsonObject = new JSONObject(testDataMap);
            /* 리눅스에 심어서 할 경우에 대비한 로컬IP */
            String local = getIP() + ":8080";

            URL urlEdge = new URL("http://" + ip + "/remote_metering/insert_data");

            int responseCodeEdge = getResponseCode(jsonObject, urlEdge);
            if (responseCodeEdge == 400) {
                logger.error("엣지서버 400 에러 : 명령 실행 오류");
            } else if (responseCodeEdge == 500) {
                logger.error("엣지서버 500 에러 : 서버 에러");
            } else if (responseCodeEdge == 200) {
                logger.info("엣지서버로 테스트 데이터를 정상적으로 송신했습니다. 응답코드 : {}", responseCodeEdge);
            } else {
                logger.error("{} : 엣지서버 통신 응답코드", responseCodeEdge);
            }

            logger.info("############ SEND TEST DATA LBEMS #############");
            logger.info("ID : {}, TOTAL_WH : {}", ((Map<?, ?>)solarMap.get("9996_1")).get("sensor_sn"), ((Map<?, ?>)solarMap.get("9996_1")).get("total_wh"));
            URL urlLbems = new URL("http://fep-lb.4st.co.kr:8080/remote_metering/insert_data");

            int responseCodeLbems = getResponseCode(jsonObject, urlLbems);
            if(responseCodeLbems == 400) {
                logger.error("lbems서버 400 에러 : 명령 실행 오류");
            } else if(responseCodeLbems == 500) {
                logger.error("lbems서버 500 에러 : 서버 에러");
            } else if (responseCodeLbems == 200) {
                logger.info("lbems서버로 테스트 데이터를 정상적으로 송신했습니다. 응답코드 : {}", responseCodeLbems);
            } else {
                logger.error("{} : lbems서버 통신 응답코드", responseCodeLbems);
            }

        } catch (Exception e) {
            logger.error("서버로 테스트 데이터를 전송하는 도중에 오류가 발생했습니다. JAVA 오류 메시지 : {}", e.getMessage());
        }
    }

    private int getResponseCode(JSONObject jsonObject, URL url) throws IOException {
        String Authorization = "myusername:mypassword";
        String base64ClientCredentials = new String(Base64.encodeBase64(Authorization.getBytes()));

        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestMethod("POST");
        http.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
        http.setRequestProperty("Authorization", "Basic " + base64ClientCredentials);
        http.setDoOutput(true);

        BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(http.getOutputStream()));
        bw.write(jsonObject.toString());
        bw.flush();
        bw.close();

        return http.getResponseCode();
    }

    private String getIP() {
        String hostAddr = null;

        try {
            Enumeration<NetworkInterface> nienum = NetworkInterface.getNetworkInterfaces();
            while (nienum.hasMoreElements()) {
                NetworkInterface ni = nienum.nextElement();
                Enumeration<InetAddress> kk= ni.getInetAddresses();
                while (kk.hasMoreElements()) {
                    InetAddress inetAddress = kk.nextElement();
                    if (!inetAddress.isLoopbackAddress() &&
                            !inetAddress.isLinkLocalAddress() &&
                            inetAddress.isSiteLocalAddress()) {
                        hostAddr = inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception e) {
            logger.error("로컬 IP를 가져오는 과정에서 오류가 발생했습니다.");
        }

        return hostAddr;
    }

    private Map<String, Object> getProperties() {
        Map<String, Object> resultMap = new HashMap<>();

        try {
            FileReader resource = new FileReader("/kevin/gs_sim_client_information.properties");
            Properties properties = new Properties();

            properties.load(resource);
            resultMap.put("edge_ip", properties.getProperty("local_server_ip"));
        } catch (Exception e) {
            logger.error("설정 파일을 읽는 도중에 오류가 발생했습니다. JAVA 오류 메시지 : {}", e.getMessage());
        }

        return resultMap;
    }
}
