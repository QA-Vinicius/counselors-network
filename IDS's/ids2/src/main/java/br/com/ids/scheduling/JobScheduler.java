package br.com.ids.scheduling;

import br.com.ids.consumer.KafkaAdviceConsumer;
import br.com.ids.data.DataSaver;
import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.metrics.TimeLogger;
import br.com.ids.producer.KafkaAdviceProducer;
import br.com.ids.producer.KafkaFeedbackProducer;
import br.com.ids.service.DetectorProcessor;
import br.com.ids.service.SampleProcessor;
import br.com.ids.data.DataLoader;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import weka.core.Instances;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Configuration
public class JobScheduler {

    private final DetectorProcessor detectorProcessor;
    private final SampleProcessor sampleProcessor;
    private final DataLoader dataLoader;
    private final DataSaver dataSaver;
    TimeLogger timeLogger;


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

        int[] oneR_Detector2 = new int[]{};

        /*
         * Nesta etapa instanciamos o primeiro Detector e seus respectivos dataSets de treino, avaliação e testes
         * essa etapa deve ser iniciada ao instanciar um IDS
         * */
        Instances trainInstances = dataLoader.leadAndFilter(false, "c2-train.arff", oneR_Detector2);
        Instances evaluationInstances = dataLoader.leadAndFilter(false, "c2-eval.arff", oneR_Detector2);
        Instances testInstances = dataLoader.leadAndFilter(false, "c2-test.arff", oneR_Detector2);

        detector = new Detector(kafkaAdviceProducer, kafkaFeedbackProducer, trainInstances, evaluationInstances, testInstances, NORMAL_CLASS);

        // Metodo para abstrair classes do CSV
        detector.loadClassValues("c2-train.arff");
        // Instancia a quantidade  clusters
        detector.createClusters(5, 2);

        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  DETECTOR 2");
        System.out.println("------------------------------------------------------------------------");

        // Zera todas as variaveis para avaliação
        detector.resetConters();

        // Cria os arquivos que seram populados com dados ao longo da execucao
        dataSaver.createEvaluationPerformanceCSV("evaluationResultsReport.csv");
        dataSaver.createTestPerformanceCSV("testResultsReport.csv");
        dataSaver.createCalculatedTestMetrics("testMetrics.csv");
        dataSaver.createCalculatedRetestMetrics("retestMetrics.csv");
        dataSaver.createCalculatedAdviceMetrics("advicesMetrics.csv");

        // Treina seus classificadores com o dataset de treino
        TimeLogger.start("Initial Training Stage");
        detector = detectorProcessor.trainingStage(detector, false);
        TimeLogger.stop("Initial Training Stage");

        detector = detectorProcessor.evaluationStage("Evaluation Stage - Before Advice", detector, false, true);

        detector = detectorProcessor.testStage("Testing Stage", detector, true, false, true, oneR_Detector2);
    }

    public void processSample(ConselorsDTO request) throws Exception {
        sampleProcessor.processSample(request, detector);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO) throws Exception {
        sampleProcessor.learnWithAdvice(conselorsDTO, detector);
    }

    public void processAdvice(ConselorsDTO advice) throws Exception {
        sampleProcessor.addSampleAndCalculateMetrics(advice, detector);
    }

    public void analyzeFinalPerformance() throws Exception {
        sampleProcessor.analyzeFinalPerformance(detector);
    }
}