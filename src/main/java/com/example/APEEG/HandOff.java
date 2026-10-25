package com.example.APEEG;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
public class HandOff {

	public static void main(String[] args) {

		System.out.println("========================================");
		System.out.println("CHECKING APPLICATION.PROPERTIES");
		System.out.println("========================================");

		try {
			ClassPathResource resource = new ClassPathResource("application.properties");

			System.out.println("File exists: " + resource.exists());
			System.out.println("File location: " + resource.getURL());

			BufferedReader reader = new BufferedReader(
					new InputStreamReader(
							resource.getInputStream(),
							StandardCharsets.UTF_8
					)
			);

			String line;

			while ((line = reader.readLine()) != null) {
				if (line.startsWith("spring.data.mongodb.uri=")) {
					System.out.println("MongoDB property FOUND:");
					System.out.println("spring.data.mongodb.uri=***HIDDEN***");
				}
			}

			reader.close();

		} catch (Exception e) {
			System.out.println("ERROR READING application.properties:");
			e.printStackTrace();
		}

		System.out.println("========================================");

		SpringApplication.run(HandOff.class, args);
	}
}