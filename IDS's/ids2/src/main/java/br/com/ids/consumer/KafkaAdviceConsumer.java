package br.com.ids.consumer;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.service.AdviceResponseCache;
import br.com.ids.service.AdviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class KafkaAdviceConsumer {

    @Autowired
    private AdviceService adviceService;

    @Autowired
    private AdviceResponseCache responseCache;

    private final Logger logg = LoggerFactory.getLogger(KafkaAdviceConsumer.class);

    @KafkaListener(topics = {"ADVICE_TOPIC"}, groupId = "myGroup2", containerFactory = "jsonKafkaListenerContainer")
    public void consumer(ConsumerRecord<String, ConselorsDTO> record) throws Exception {
        logg.info("Received Message from Partition: " + record.partition() + ", Offset: " + record.offset());
        final var time = System.currentTimeMillis();

        System.out.println("\n\t---------------------- NEW MESSAGE ----------------------");
        System.out.println("\tBy: Counselor " + record.value().getId_conselheiro());
        System.out.println("\tMessage Type: " + record.value().getFlag());
        System.out.println("\tID Sample: " + record.value().getId_sample());

        if(!record.value().getId_conselheiro().equals("2")){
            if (record.value().getFlag().equals("REQUEST_ADVICE")) {
                try{
                    adviceService.generatesAdvice(record.value());
                }catch(Exception ex){
                    throw ex;
                }
            }
            if (record.value().getFlag().equals("RESPONSE_ADVICE")) {
                try{
                    responseCache.storeAdvice(record.value());

                    if(responseCache.stoppingCriterion(record.value().getId_sample())) {
                        ConselorsDTO bestAdvice = responseCache.getBestAdvice(record.value().getId_sample());
                        adviceService.learnWithAdvice(bestAdvice);
                    }
                }catch(Exception ex){
                    throw ex;
                }
            }
        } else {
            System.out.println("\tAction: Ignore own message!");
        }
        System.out.println("\t---------------------------------------------------------\n");
    }
}