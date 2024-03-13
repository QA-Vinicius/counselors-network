package br.com.ids.producer;

import br.com.ids.dto.ConselorsDTO;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaAdviceProducer {

    private final String topic;
    private final Logger logg = LoggerFactory.getLogger(KafkaAdviceProducer.class);
    @Autowired
    private final KafkaTemplate<String, ConselorsDTO> kafkaTemplate;

    public KafkaAdviceProducer(KafkaTemplate<String, ConselorsDTO> kafkaTemplate){
        this.topic = "ADVICE_TOPIC";
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(ConselorsDTO data){
        kafkaTemplate.send(topic, data);
    }
}