package com.sqlbot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SqlBotAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(SqlBotAiApplication.class, args);
        System.out.println("========================================");
        System.out.println("  AI\u6570\u636e\u8d44\u4ea7\u5e73\u53f0\u542f\u52a8\u6210\u529f!");
        System.out.println("  \u8bbf\u95ee\u5730\u5740: http://localhost:8080");
        System.out.println("========================================");
    }
}
