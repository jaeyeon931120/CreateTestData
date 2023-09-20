package com.kevin.edge_solar.service;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.*;

public class SolarTestData {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private Random random = new Random();
    public Map<String, Object> createSolar() {
        Map<String, Object> resultMap = new HashMap<>();
        Map<String, Object> solarMap = new HashMap<>();
        Map<String, Object> allDataMap = new HashMap<>();

        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            Calendar cal = Calendar.getInstance(Locale.KOREA);
            String val_date = sdf.format(cal.getTime());

            long todaymill = System.currentTimeMillis();
            String strMill = Long.toString(todaymill);
            strMill = strMill.substring(4, strMill.length() -1);
            String val = String.valueOf((long) Math.floor((double) Long.parseLong(strMill) / 1000));
            int solarCount;
            int ran_int = random.nextInt(100);
            double ran_dobule = random.nextDouble();

            solarCount = ran_int;
            BigDecimal efficiency = BigDecimal.valueOf((ran_dobule * 9999999999999L) / 10000000000000L);

            allDataMap.put("efficiency", efficiency);
            JSONObject jsonObject = new JSONObject(allDataMap);

            int createNum;  	//1자리 난수
            String ranNum; 		//1자리 난수 형변환 변수
            int letter;			//난수 자릿수
            StringBuilder current_w = new StringBuilder();  		//결과 난수

            if(solarCount <= 50) {
                letter = 4;
            } else {
                letter = 5;
            }

            for (int i=0; i<letter; i++) {
                createNum = random.nextInt(9);		//0부터 9까지 올 수 있는 1자리 난수 생성
                ranNum =  Integer.toString(createNum);  //1자리 난수를 String으로 형변환
                current_w.append(ranNum);			//생성된 난수(문자열)을 원하는 수(letter)만큼 더하며 나열
            }

            solarMap.put("sensor_sn", "9996_1");
            solarMap.put("complex_code_pk", null);
            solarMap.put("home_dong_pk", null);
            solarMap.put("home_ho_pk", null);
            solarMap.put("val_date", val_date);
            solarMap.put("total_wh", val);
            solarMap.put("val", val);
            solarMap.put("w", current_w.toString());
            solarMap.put("all_data", jsonObject.toString());
            solarMap.put("error_code", "0");
            solarMap.put("capacity", 46);

            resultMap.put("9996_1", solarMap);
        } catch (Exception e) {
            logger.error("태양광 테스트 데이터 생성 도중 오류가 발생했습니다. JAVA 오류 원인 : {}", e.getMessage());
            resultMap.put("error_code", 1);
        }

        return resultMap;
    }
}
