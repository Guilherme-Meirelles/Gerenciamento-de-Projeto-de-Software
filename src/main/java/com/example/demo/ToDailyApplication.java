package com.example.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.Arrays;

@SpringBootApplication
public class ToDailyApplication {

	public static void main(String[] args) {
		SpringApplication.run(ToDailyApplication.class, Arrays.toString(args));
	}

}