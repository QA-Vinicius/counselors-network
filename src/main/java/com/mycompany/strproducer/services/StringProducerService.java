package com.mycompany.strproducer.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mycompany.dto.ConselorsDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Log4j2
@RequiredArgsConstructor
@Service
public class StringProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaMessageProducer kafkaMessageProducer;

    public void sendMessage(String message) {
        kafkaTemplate.send("network-topic", message).addCallback(
            success -> {
                assert success != null;
                log.info("Sucesso ao enviar mensagem {}", success.getProducerRecord().value());
                log.info("Partition {}, Offset {}",
                    success.getRecordMetadata().partition(),
                    success.getRecordMetadata().offset());
            },
            error -> log.error("Erro ao enviar mensagem {}", error.getMessage())
        );
    }

    public void sendAdviceRequest(ConselorsDTO conselorsDTO) {
        try {
            String message = new ObjectMapper().writeValueAsString(conselorsDTO);
            kafkaTemplate.send("network-topic", message).addCallback(
                success -> log.info("Sucesso ao enviar mensagem {}", message),
                error -> log.error("Erro ao enviar mensagem {}", message)
            );
        } catch (JsonProcessingException e) {
            // Trate o erro, caso ocorra
            e.printStackTrace();
        }
    }
}
