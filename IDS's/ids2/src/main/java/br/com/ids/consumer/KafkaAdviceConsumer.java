package br.com.ids.consumer;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.service.AdviceResponseCache;
import br.com.ids.service.AdviceService;
import br.com.ids.service.SampleProcessor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static br.com.ids.service.ConflictService.consumerStoppingCriterion;

@Component
@Slf4j
public class KafkaAdviceConsumer {

    @Autowired
    private AdviceService adviceService;

    @Autowired
    private AdviceResponseCache responseCache;

    private final ExecutorService executorService = Executors.newSingleThreadExecutor(); // apenas uma thread para evitar sobrecarga
    private final LinkedBlockingQueue<ConselorsDTO> adviceQueue = new LinkedBlockingQueue<>();

    // Variaveis para determinar o criterio de parada do consumer para o RESPONSE_ADVICE
    private int responseAdviceCount = 0;

    // Variavel para contabilizar as amostras ja processadas
    private final Set<Integer> processedSamples = ConcurrentHashMap.newKeySet();

    private final Logger logg = LoggerFactory.getLogger(KafkaAdviceConsumer.class);

    @KafkaListener(topics = {"ADVICE_TOPIC"}, groupId = "myGroup2", containerFactory = "jsonKafkaListenerContainer")
    public void consumer(ConsumerRecord<String, ConselorsDTO> record) throws Exception {
        try {
            logg.info("Received Message from Partition: " + record.partition() + ", Offset: " + record.offset());

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
                    int id_sample = record.value().getId_sample();

                    if(!processedSamples.contains(id_sample)) {
                        try {
                            responseCache.storeAdvice(record.value());

                            if (responseCache.stoppingCriterion(record.value().getId_sample())) {
                                ConselorsDTO bestAdvice = responseCache.getBestAdvice(record.value().getId_sample());
                                processedSamples.add(id_sample);
//                                adviceService.learnWithAdvice(bestAdvice);
                            }

                            responseAdviceCount++;
                            if (responseAdviceCount >= consumerStoppingCriterion()) {
                                logg.info("Received all possible RESPONSE_ADVICE messages, stopping consumer!");

                                // Avaliar como ficou o detector apos os aprendizados com conselhos
                                adviceService.analyzeFinalPerformance(record.value());
                                return;
                            }
                        } catch (Exception ex) {
                            throw ex;
                        }
                    } else {
                        System.out.println("\tThe advice for this sample (" + id_sample + ") has already been processed!");
                    }
                }
            } else {
                System.out.println("\tAction: Ignore own message!");
            }
            System.out.println("\t---------------------------------------------------------\n");
        } catch (Exception e) {
            logg.error("Error processing message ", e);
        }
    }

    private void asyncLearning(ConselorsDTO advice) {
        adviceQueue.offer(advice);
        if (!executorService.isShutdown()) {
            executorService.submit(() -> {
                while (!adviceQueue.isEmpty()) {
                    ConselorsDTO adviceToLearn = adviceQueue.poll();
                    // Implementação do treinamento em thread separada
                    try {
                        adviceService.retrainWithAdvice(adviceToLearn);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        }
    }
}