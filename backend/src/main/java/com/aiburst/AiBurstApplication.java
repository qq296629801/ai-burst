package com.aiburst;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan({"com.aiburst.mapper", "com.aiburst.llm.mapper", "com.aiburst.mag.mapper"})
public class AiBurstApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiBurstApplication.class, args);
    }
}
