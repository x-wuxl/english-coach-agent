package com.wuxl.englishcoach;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wuxl.englishcoach.infrastructure.persistence")
public class EnglishCoachApplication {

    public static void main(String[] args) {
        SpringApplication.run(EnglishCoachApplication.class, args);
    }
}
