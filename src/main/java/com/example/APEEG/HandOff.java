package com.example.APEEG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class HandOff {

	public static void main(String[] args) {
		SpringApplication.run(HandOff.class, args);
	}

}
