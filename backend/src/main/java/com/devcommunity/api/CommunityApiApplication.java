package com.devcommunity.api;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.devcommunity.api.dao")
public class CommunityApiApplication {
    public static void main(String[] args) {
        SpringApplication.run(CommunityApiApplication.class, args);
    }
}
