package br.com.ids;

import br.com.ids.consumer.KafkaAdviceConsumer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class IntrusionDetectionApplication {
	public static void main(String[] args) {
		SpringApplication.run(IntrusionDetectionApplication.class, args);
	}
}