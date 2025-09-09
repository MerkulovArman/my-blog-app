package org.example.blogtestapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MyBlogAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyBlogAppApplication.class, args);
    }

}
