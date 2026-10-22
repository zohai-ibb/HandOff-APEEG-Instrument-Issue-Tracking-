package com.example.APEEG;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableMongoAuditing
public class HandOff {

	public static void main(String[] args) {
		// Load environment variables from .env file if it exists
		try {
			Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();
			dotenv.entries().forEach(entry -> {
				System.setProperty(entry.getKey(), entry.getValue());
			});
		} catch (Exception e) {
			System.out.println(".env file not found, falling back to system environment variables.");
		}

		SpringApplication.run(HandOff.class, args);
	}

}
