package org.example.blogtestapp;

import org.springframework.boot.SpringApplication;

public class TestMyBlogAppApplication {

    public static void main(String[] args) {
        SpringApplication.from(MyBlogAppApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
