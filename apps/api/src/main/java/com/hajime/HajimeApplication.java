package com.hajime;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HajimeApplication {
  public static void main(String[] args) {
    SpringApplication.run(HajimeApplication.class, args);
  }
}
