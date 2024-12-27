package br.com.ids.scheduling;

import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.producer.KafkaAdviceProducer;
import br.com.ids.producer.KafkaFeedbackProducer;
import br.com.ids.service.DetectorProcessor;
import br.com.ids.service.SampleProcessor;
import br.com.ids.util.DataLoader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import weka.core.Instances;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static br.com.ids.consumer.KafkaAdviceConsumer.formato_br;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Configuration
public class JobScheduler {

    private final DetectorProcessor detectorProcessor;
    private final SampleProcessor sampleProcessor;
    private final DataLoader dataLoader;

    static final String NORMAL_CLASS = "BENIGN";


    @Autowired
    private BeanFactory beanFactory;

    private Detector detector;

    @PostConstruct
    public void initialize() throws Exception {
        startProcess();
    }

//    @Scheduled(cron = "0 */1 * * * *", zone = "America/Sao_Paulo")
    public void startProcess() throws Exception {
        KafkaTemplate<String, ConselorsDTO> kafkaTemplate = beanFactory.getBean(KafkaTemplate.class);
        KafkaAdviceProducer kafkaAdviceProducer = beanFactory.getBean(KafkaAdviceProducer.class);
        KafkaFeedbackProducer kafkaFeedbackProducer = beanFactory.getBean(KafkaFeedbackProducer.class);

        int[] oneR_Detector3 = new int[]{};

        /*
         * Nesta etapa instanciamos o primeiro Detector e seus respectivos dataSets de treino, avaliação e testes
         * essa etapa deve ser iniciada ao instanciar um IDS
         * */
        Instances trainInstances = dataLoader.leadAndFilter(false, "c3-train.arff", oneR_Detector3);
        Instances evaluationInstances = dataLoader.leadAndFilter(false, "c3-eval.arff", oneR_Detector3);
        Instances testInstances = dataLoader.leadAndFilter(false, "c3-test.arff", oneR_Detector3);

        detector = new Detector(kafkaAdviceProducer, kafkaFeedbackProducer, trainInstances, evaluationInstances, testInstances, NORMAL_CLASS);

        // Metodo para abstrair classes do CSV
        detector.loadClassValues("c3-train.arff");
        // Instancia a quantidade  clusters
        detector.createClusters(5, 2);

        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  DETECTOR 3");
        System.out.println("------------------------------------------------------------------------");

        // Zera todas as variaveis para avaliação
        detector.resetConters();

        //Treina seus classificadores com o dataset de treino
        Instant inicio = Instant.now();
        LocalDateTime horaInicio = LocalDateTime.ofInstant(inicio, ZoneId.systemDefault());
        System.out.println("[IDS 3] Inicio Treino: " + horaInicio.format(formato_br));
        detector = detectorProcessor.trainingStage(detector, false);
        Instant fim = Instant.now();
        LocalDateTime horaFim = LocalDateTime.ofInstant(fim, ZoneId.systemDefault());
        System.out.println("[IDS 3] Fim Treino: " + horaFim.format(formato_br));
        Duration duracao = Duration.between(inicio, fim);

        Instant inicioEval = Instant.now();
        LocalDateTime horaInicioEval = LocalDateTime.ofInstant(inicioEval, ZoneId.systemDefault());
        System.out.println("\n[IDS 3] Inicio Eval: " + horaInicioEval.format(formato_br));
        detector = detectorProcessor.evaluationStage(detector, false, true);
        Instant fimEv = Instant.now();
        LocalDateTime horaFimEv = LocalDateTime.ofInstant(fimEv, ZoneId.systemDefault());
        System.out.println("[IDS 3] Fim Eval: " + horaFimEv.format(formato_br));

        Duration duracaoEv = Duration.between(inicioEval, fimEv);

        System.out.println("\n\n[IDS 3] Inicio Treino: " + horaInicio.format(formato_br));
        System.out.println("[IDS 3] Fim Treino: " + horaFim.format(formato_br));
        System.out.println("Tempo de Treino: " + duracao.getSeconds() + " segundos");


        System.out.println("\n[IDS 3] Inicio Eval: " + horaInicioEval.format(formato_br));
        System.out.println("[IDS 3] Fim Eval: " + horaFimEv.format(formato_br));
        System.out.println("Tempo de Eval: " + duracaoEv.getSeconds() + " segundos");
//        detector = detectorProcessor.testStage(detector, true, false, true, oneR_Detector3);
//        System.out.println("FIM TREINO AVALIAÇÃO E TESTE");
    }

    public void processSample(ConselorsDTO request) throws Exception {
        sampleProcessor.processSample(request, detector);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO) throws Exception {
        sampleProcessor.learnWithAdvice(conselorsDTO, detector);
    }
}