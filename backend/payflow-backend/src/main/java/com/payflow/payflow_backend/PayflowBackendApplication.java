package com.payflow.payflow_backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class PayflowBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(PayflowBackendApplication.class, args);
    }

}