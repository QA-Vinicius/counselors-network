package br.com.ids.scheduling;

import br.com.ids.data.DataSaver;
import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
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
        Instances trainInstances = dataLoader.leadAndFilter(false, "4output1k.csv", oneR_Detector2);
        Instances evaluationInstances = dataLoader.leadAndFilter(false, "5output1k.csv", oneR_Detector2);
        Instances testInstances = dataLoader.leadAndFilter(false, "6output1k.csv", oneR_Detector2);

        detector = new Detector(kafkaAdviceProducer, kafkaFeedbackProducer, trainInstances, evaluationInstances, testInstances, NORMAL_CLASS);

        // Metodo para abstrair classes do CSV
        detector.loadClassValues("4output1k.csv");
        // Instancia a quantidade  clusters
        detector.createClusters(5, 2);

        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  DETECTOR 2");
        System.out.println("------------------------------------------------------------------------");

        // Zera todas as variaveis para avaliação
        detector.resetConters();

        // Cria o arquivo previamente que sera populado com os f1scores apos o aprendizado com cada conselho
        dataSaver.createPerformanceCSV("resultsReport.csv");

        // Treina seus classificadores com o dataset de treino
        detector = detectorProcessor.trainingStage(detector, false);
        detector = detectorProcessor.evaluationStage("Evaluation Stage - Before Advice", detector, false, true);
        detector = detectorProcessor.testStage("Testing Stage", detector, true, false, true, oneR_Detector2);
//        System.out.println("FIM TREINO AVALIAÇÃO E TESTE");

        //Chamar um reteste para avaliar como fica o f1score apos aprender com todos os conselhos que tiveram ao longo do fluxo
        // 1- adicionar criterio de parada no consumer do response_advice para nao ficar escutando infinitamente e seguir o fluxo
        // 2- avaliar se deve chamar a etapa de teste sem solicitar novos conselhos ou se solicitamos
        // se nao for para solicitar, entao devemos adicionar uma flag no testStage para determinar se esta numa etapa onde deve
        // chamar o sendRequest ou nao
    }

    public void processSample(ConselorsDTO request) throws Exception {
        sampleProcessor.processSample(request, detector);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO) throws Exception {
        sampleProcessor.learnWithAdvice(conselorsDTO, detector);
    }
}