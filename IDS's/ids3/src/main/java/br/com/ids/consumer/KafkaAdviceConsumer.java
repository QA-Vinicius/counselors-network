package br.com.ids.consumer;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.service.AdviceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class KafkaAdviceConsumer {

    @Autowired
    private AdviceService adviceService;

    public static final DateTimeFormatter formato_br = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final Set<Integer> processedSamples = ConcurrentHashMap.newKeySet();

    private final Logger logg = LoggerFactory.getLogger(KafkaAdviceConsumer.class);

    @KafkaListener(topics = {"ADVICE_TOPIC"}, groupId = "myGroup3", containerFactory = "jsonKafkaListenerContainer")
    public void consumer(ConsumerRecord<String, ConselorsDTO> record) throws Exception {
        logg.info("Received Message from Partition: " + record.partition() + ", Offset: " + record.offset());
        final var time = System.currentTimeMillis();

        System.out.println("\n\t---------------------- NEW MESSAGE ----------------------");
        System.out.println("\tBy: Counselor " + record.value().getId_conselheiro());
        System.out.println("\tMessage Type: " + record.value().getFlag());
        System.out.println("\tID Sample: " + record.value().getId_sample());

        if(!record.value().getId_conselheiro().equals("3")){
            if (record.value().getFlag().equals("REQUEST_ADVICE")) {
                Instant inicio = Instant.now();
                LocalDateTime horaInicio = LocalDateTime.ofInstant(inicio, ZoneId.systemDefault());
                System.out.println("[IDS 3] Hora de chegada do Request: " + horaInicio.format(formato_br));

                int id_sample = record.value().getId_sample();

                if(!processedSamples.contains(id_sample)) {
                    processedSamples.add(id_sample);
                    try{
                        adviceService.generatesAdvice(record.value());

                        Instant fim = Instant.now();
                        LocalDateTime horaFim = LocalDateTime.ofInstant(fim, ZoneId.systemDefault());
                        System.out.println("\n[IDS 3] Hora de envio do conselho: " + horaFim.format(formato_br));

                        // Calcula a diferença de tempo em segundos
                        Duration duracao = Duration.between(inicio, fim);
                        System.out.println("Tempo de processamento: " + duracao.getNano() + " segundos");
                    }catch(Exception ex){
                        throw ex;
                    }
                } else {
                    System.out.println("\tThis sample (" + id_sample + ") has already been processed!");
//                    logg.info("This sample (" + id_sample + ") has already been processed!");
                }
            }
            if (record.value().getFlag().equals("RESPONSE_ADVICE")) {
                try{
//                    adviceService.learnWithAdvice(record.value());
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