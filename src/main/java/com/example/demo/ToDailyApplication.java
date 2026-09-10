package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class ToDailyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToDailyApplication.class, Arrays.toString(args));
	}

}