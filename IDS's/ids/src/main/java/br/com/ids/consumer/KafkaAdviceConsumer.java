package br.com.ids.consumer;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.service.GenerateAdviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Slf4j
public class KafkaAdviceConsumer {

    @Autowired
    private GenerateAdviceService adviceService;

    private final Logger logg = LoggerFactory.getLogger(KafkaAdviceConsumer.class);

    @KafkaListener(topics = {"ADVICE_TOPIC"}, groupId = "myGroup", containerFactory = "jsonKafkaListenerContainer")
    public void consumer(ConsumerRecord<String, ConselorsDTO> record) throws Exception {
        logg.info("Received Message " + record.value());
        final var time = System.currentTimeMillis();
        if(record.value().getId_conselheiro() != 1){
                try{
                    System.out.println("CHEGUEI = " + record.value());
                    adviceService.generatesAdvice(record.value());
                }catch(Exception ex){
                    throw ex;
                }
        }
        System.out.println("CHEGUEI = " + Arrays.toString(record.value().getSample()));
    }
}
