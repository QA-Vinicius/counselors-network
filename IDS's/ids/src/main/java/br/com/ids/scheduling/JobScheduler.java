package br.com.ids.scheduling;

import br.com.ids.domain.Detector;
import br.com.ids.domain.DetectorClassifier;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.enuns.AdviceEnum;
import br.com.ids.producer.KafkaAdviceProducer;
import br.com.ids.producer.KafkaFeedbackProducer;
import br.com.ids.service.DetectorClusterService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

@Component
@EnableScheduling
@RequiredArgsConstructor
@Configuration
public class JobScheduler {

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

        int[] oneR_Detector1 = new int[]{}; //79, 40, 68, 13, 55

        /*
         * Nesta etapa instanciamos o primeiro Detector e seus respectivos dataSets de treino, avaliação e testes
         * essa etapa deve ser iniciada ao instanciar um IDS
         * */
        Instances trainInstances = leadAndFilter(false, "1output1k.csv", oneR_Detector1);
        Instances evaluationInstances = leadAndFilter(false, "2output1k.csv", oneR_Detector1);
        Instances testInstances = leadAndFilter(false, "3output1k.csv", oneR_Detector1);

        detector = new Detector(kafkaAdviceProducer, kafkaFeedbackProducer, trainInstances, evaluationInstances, testInstances, NORMAL_CLASS);

        // Metodo para abstrair classes do CSV
        detector.loadClassValues("1output1k.csv");
        // Instancia a quantidade  clusters
        detector.createClusters(5, 2);

        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  DETECTOR 1");
        System.out.println("------------------------------------------------------------------------");

        // Zera todas as variaveis para avaliação
        detector.resetConters();

        //Treina seus classificadores com o dataset de treino
        detector = trainingStage(detector, false);
        detector = evaluationStage(detector, false, true);
        detector = testStage(detector, true, false, true, oneR_Detector1);
//        System.out.println("FIM TREINO AVALIAÇÃO E TESTE");
    }

    private static Detector trainingStage(Detector detec, boolean printTrain) throws Exception {
        /* Train Phase */
        System.out.println("\t1- Training Stage");
        System.out.println("\t\tTraining with " + detec.trainInstances.numInstances() + " instances.");
        detec.trainClassifiers(printTrain);

        System.out.println("\n\tEnd of training stage");
        System.out.println("------------------------------------------------------------------------");

        return detec;
    }

    private static Detector evaluationStage(Detector detec, boolean printEvaluation, boolean showProgress) throws Exception {
        /* Evaluation Phase */
        System.out.println("\t2- Evaluation Stage");
        detec.evaluateClassifiersPerCluster(printEvaluation, showProgress);

        System.out.println("\n\tEnd of evaluation stage");
        System.out.println("------------------------------------------------------------------------");

        return detec;
    }

    private static Detector testStage(Detector detec, boolean advices, boolean printEvaluation, boolean showProgress, int[] features) throws Exception {
        /* Evaluation Phase */
        System.out.println("\t3- Testing Stage");
        detec.clusterAndTestSample(advices, true, true, printEvaluation, showProgress, features, AdviceEnum.REQUEST_ADVICE);

        System.out.println("\n\tEnd of testing stage");
        System.out.println("------------------------------------------------------------------------");

        return detec;
    }

    public static Instances leadAndFilter(boolean printSelection, String file, int[] featureSelection) throws Exception {
        Instances instances = new Instances(readDataFile(file));
        if (featureSelection.length > 0) {
            instances = applyFilterKeep(instances, featureSelection);
            if (printSelection) {
                System.out.println(Arrays.toString(featureSelection) + " - ");
            }
        }
        return instances;
    }

    public static BufferedReader readDataFile(String filename) {
        BufferedReader inputReader = null;

        try {
            inputReader = new BufferedReader(new FileReader(filename));
        } catch (FileNotFoundException ex) {
            System.err.println("File not found: " + filename);
        }

        return inputReader;
    }

    public static Instances applyFilterKeep(Instances instances, int[] fs) {
        Arrays.sort(fs);
        for (int i = instances.numAttributes() - 1; i > 0; i--) {
            if (instances.numAttributes() <= fs.length) {
                System.err.println("O número de features (" + instances.numAttributes() + ") precisa ser maior que o filtro (" + fs.length + ").");
                return instances;
            }
            boolean deletar = true;
            for (int j : fs) {
                if (i == j) {
                    deletar = false;
//                    System.out.println("Manter [" + i + "]:" + instances.attribute(i));
                }
            }
            if (deletar) {
                instances.deleteAttributeAt(i - 1);
            }
        }
        return instances;
    }

    public void processSample(ConselorsDTO request) throws Exception {
        System.out.println("\tAction: Generate Advice\n");
        detector.onAdviceRequest(request);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO) throws Exception {
        System.out.println("\tAction: Learn With Advice\n");
        if (detector == null) {
            throw new IllegalStateException("\t[ERROR] Detector is not initialized.");
        }

        Instances trainInstances = detector.getTrainInstances();
        if (trainInstances.classIndex() == -1) {
            trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
        }

        System.out.println("\t1- Extracting the received sample and label");
        double[] sample = conselorsDTO.getSample();
        double sampleLabel = conselorsDTO.getResult();
        double[] values = Arrays.copyOf(sample, sample.length + 1); // Adiciona espaço para o atributo de classe
        values[values.length - 1] = sampleLabel; // Valor inicial para o atributo de classe

        System.out.println("\t2- Creating new instance with labeled sample");
        Instance newInstance = new DenseInstance(1.0, values);
        newInstance.setDataset(trainInstances);

        System.out.println("\t3- Adding instance to trainInstances");
        trainInstances.add(newInstance);

//        System.out.println("TAMANHO INSTANCIA DE TREINO: " + trainInstances.numAttributes());

        System.out.println("\t4- Retraining the classifiers");
        detector = trainingStage(detector, false);
        detector = evaluationStage(detector, false, true);
    }
}