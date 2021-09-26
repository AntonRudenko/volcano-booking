package com.upgrade.volcano;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@EnableCaching
@SpringBootApplication
public class VolcanoApplication {

    public static void main(String[] args) {
        SpringApplication.run(VolcanoApplication.class, args);
    }

}
