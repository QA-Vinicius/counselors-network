package br.com.ids;

import br.com.ids.domain.Detector;
import br.com.ids.domain.DetectorClassifier;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.enuns.AdviceEnum;
import br.com.ids.producer.KafkaAdviceProducer;
import br.com.ids.scheduling.JobScheduler;
import br.com.ids.service.DetectorClusterService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

@SpringBootApplication
@EnableScheduling
public class IntusionDetectionApplication {
	public static void main(String[] args) {
		SpringApplication.run(IntusionDetectionApplication.class, args);
	}
}