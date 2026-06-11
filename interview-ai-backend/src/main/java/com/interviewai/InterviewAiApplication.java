package com.interviewai;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.interviewai.mapper")
public class InterviewAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(InterviewAiApplication.class, args);
    }
}
