package br.com.ids;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class IntrusionDetectionApplication {
	public static void main(String[] args) {
		SpringApplication.run(IntrusionDetectionApplication.class, args);
	}
}