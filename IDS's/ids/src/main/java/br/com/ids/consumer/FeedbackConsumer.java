package br.com.ids.consumer;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.service.FeedbackService;
import br.com.ids.service.GenerateAdviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FeedbackConsumer {

    @Autowired
    private FeedbackService feedbackService;

    private final Logger logg = LoggerFactory.getLogger(KafkaAdviceConsumer.class);

    @KafkaListener(topics = {"FEEDBACK_TOPIC"}, groupId = "myGroup", containerFactory = "jsonKafkaListenerContainer")
    public void consumerFeedBack(ConsumerRecord<String, ConselorsDTO> record){
        logg.info("Received Message " + record.value());
        final var time = System.currentTimeMillis();
        if(record.value().getId_conselheiro() == 1){
            try{
                feedbackService.learnAndRetrain(record.value());
            }catch(Exception ex){
                throw ex;
            }
        }
        else {
            try {
                feedbackService.learnWithFeedback(record.value());
            } catch (Exception ex) {
                throw ex;
            }
        }
    }
}
