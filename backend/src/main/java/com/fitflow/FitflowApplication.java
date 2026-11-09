package com.fitflow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
/** Spring Boot 服务入口，负责启动健身房管理系统后端。 */
public class FitflowApplication {
    public static void main(String[] args) {
        SpringApplication.run(FitflowApplication.class, args);
    }
}
