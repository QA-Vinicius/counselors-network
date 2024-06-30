package br.com.ids.scheduling;

import br.com.ids.IntusionDetectionApplication;
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
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
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
        startTraining();
    }

    @Scheduled(cron = "0 */2 * * * *", zone = "America/Sao_Paulo")
    public void startTraining() throws Exception {
        System.out.println("INICIANDO PROCEDIMENTO DE TREINO");
        KafkaTemplate<String, ConselorsDTO> kafkaTemplate = beanFactory.getBean(KafkaTemplate.class);
        KafkaAdviceProducer kafkaAdviceProducer = beanFactory.getBean(KafkaAdviceProducer.class);
        KafkaFeedbackProducer kafkaFeedbackProducer = beanFactory.getBean(KafkaFeedbackProducer.class);

        int[] oneR_Detector2 = new int[]{4, 48, 8, 12, 33, 40, 79}; //79, 40, 68, 13, 55

        /*
         * Nesta etapa instanciamos o primeiro Detector e seus respectivos dataSets de treino, avaliação e testes
         * essa etapa deve ser iniciada ao instanciar um IDS
         * */
        Instances trainInstances = leadAndFilter(false, "4output1k.csv", oneR_Detector2);
        Instances evaluationInstances = leadAndFilter(false, "5output1k.csv", oneR_Detector2);
        Instances testInstances = leadAndFilter(false, "6output1k.csv", oneR_Detector2);

        detector = new Detector(kafkaAdviceProducer, kafkaFeedbackProducer, 2, trainInstances, evaluationInstances, testInstances, NORMAL_CLASS);

        // Instancia a quantidade  clusters
        detector.createClusters(5, 2);

        System.out.println("\n######## Detector 2");

        // Zera todas as variaveis para avaliação
        detector.resetConters();
        System.out.println("FIM1");

        //Treina seus classificadores com o dataset de treino
        detector = trainEvaluateAndTest(detector, false, false, true, true, oneR_Detector2);
        System.out.println("FIM2");
    }

    private static Detector trainEvaluateAndTest(Detector D2, boolean printEvaluation, boolean printTrain, boolean advices, boolean showProgress, int[] features) throws Exception {
        /* Train Phase*/
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Train");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Treinamento com " + D2.trainInstances.numInstances() + " instâncias.");
        D2.trainClassifiers(printTrain);
        System.out.println("FIM Classifiers");

        /* Evaluation Phase */
        D2.evaluateClassifiersPerCluster(printEvaluation, showProgress);
        System.out.println("FIM evaluate");

        /* Test Phase */
        D2.clusterAndTestSample(advices, true, true, printEvaluation, showProgress, features, AdviceEnum.REQUEST_ADVICE);
        System.out.println("FIM samples");
//        int VP = 0;
//        int VN = 0;
//        int FP = 0;
//        int FN = 0;
        for (DetectorClusterService d : D2.getClusters()) {
            System.out.println("---- Cluster " + d.getClusterNum() + ":");
            for (DetectorClassifier c : d.getClassifiers()) {
                if (c.isSelected()) {
                    System.out.println("[X]" + c.getName()
                            + " - " + c.getEvaluationF1Score() // antes era + " - " + c.getTestAccuracy()
                            + " (VP;VN;FP;FN) = "
                            + "("
                            + c.getVP()
                            + ";" + c.getVN()
                            + ";" + c.getFP()
                            + ";" + c.getFN()
                            + ") = ("
                            + (c.getVP() + c.getVN() + c.getFP() + c.getFN())
                            + "/" + D2.getCountTestInstances() + ")");
                    /* Atualiza Totais*/
                }

            }

        }
        System.out.println("FIM for");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Test Summary: [Solucionados "+ D2.getGoodAdvices()+"/"+ D2.getConflitos() + " conflitos de " + (D2.getVP() + D2.getVN() + D2.getFP() + D2.getFN()) + " classificações.] \n "
                + "VP	VN	FP	FN	F1Score \n"
                + D2.getVP() + ";" + D2.getVN() + ";" + D2.getFP() + ";" + D2.getFN() + ";" + String.valueOf(D2.getDetectionF1Score()).replace(".", ","));
        System.out.println("------------------------------------------------------------------------");

        System.out.println("FIM treino");
        return D2;
    }

    public void processNewSample(ConselorsDTO conselorsDTO) throws Exception {
        if (detector == null) {
            throw new IllegalStateException("Detector is not initialized.");
        }

        Instances trainInstances = detector.getTrainInstances();
        if (trainInstances.classIndex() == -1) {
            trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
        }

        double[] sample = conselorsDTO.getSample();
        double[] values = Arrays.copyOf(sample, sample.length + 1); // Adiciona espaço para o atributo de classe
        values[values.length - 1] = Double.NaN; // Valor inicial para o atributo de classe

        Instance newInstance = new DenseInstance(1.0, values);
        newInstance.setDataset(trainInstances);
        trainInstances.add(newInstance);

        // Reavalia e treina novamente
        trainEvaluateAndTest(detector, false, false, true, true, new int[]{4, 48, 8, 12, 33, 40, 79});

        // Classifica a nova instância
        double classValue = detector.classifyInstance(newInstance);
        System.out.println("Class Value: " + classValue);
    }

    public Instances leadAndFilter(boolean printSelection, String file, int[] featureSelection) throws Exception {
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
}