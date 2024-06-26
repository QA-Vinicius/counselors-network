package br.com.ids.service;

import br.com.ids.domain.Detector;
import br.com.ids.domain.DetectorClassifier;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.enuns.AdviceEnum;
import br.com.ids.util.GenerateReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

@Service
public class GenerateAdviceService {

    @Autowired(required = true)
    private KafkaTemplate<String, ConselorsDTO> kafkaTemplate;

    Detector detector;

    public GenerateAdviceService() {
    }

    @SuppressWarnings("")
    public void generatesAdvice(ConselorsDTO value) throws Exception { //responsavel por gerar um conselho para o IDS1
        // Gerar conselho
        /*Aplicar o Kmeans  weka.clusterers.SimpleKMeans.clusterInstance
        *   A partir de features analisadas em conselho(sample)
        *       int cluster = kmeans.cluster(sample)
        *       var selectedClassfier[] = getBestClassofier(cluster) com.mycompany.counselorsnetwork.DetectorClassifier
        *       result = selectedClassfier.classify (> 1 utilizar laço de repetição)
        *       avaliar respostas dos classificadores, e seguir com a resposta majoritaria
        *       kafka.send.(resposta)
        * */

        // Criando um array de atributos
//        DenseInstance inst = generateInstance(value);
        System.out.println("Amostra recebida: " + Arrays.toString(value.getSample()));
        Instances instances = new Instances(GenerateReader.generateInstances(value.getSample()));
        for (int i = 0; i<=instances.size(); i++) {
            detector.trainInstances.add(instances.get(i));
        }
        detector.trainClassifiers(false);
        detector.evaluateClassifiersPerCluster(true, false);
        detector.selectClassifierPerCluster(false);
    }

//    public static DenseInstance generateInstance(ConselorsDTO value) {
//        ArrayList<Attribute> attributes = new ArrayList<> ();
//        for (int i = 0; i < value.getSample().length; i++) {
//            attributes.add(new Attribute("atributo" + i));
//        }
//
//        // Criando uma instância com base nos atributos
//        Instances dataset = new Instances("my_dataset", attributes  , 1);
//        dataset.setClassIndex(dataset.numAttributes() - 1); // Definindo o índice da classe (último atributo)
//
//        // Criando a instância
//        DenseInstance inst = new DenseInstance(value.getSample().length);
//        inst.setDataset(dataset);
//
//        // Definindo os valores para a instância
//        Arrays.stream(value.getSample()).forEach(it ->{
//            int count = 0;
//            inst.setValue(count++,it);} );
//        return inst;
//    }

    private static Detector trainEvaluateAndTest(Detector D1, boolean printEvaluation, boolean printTrain, boolean advices, boolean showProgress, int[] features) throws Exception {
        /* Train Phase*/
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Train");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Treinamento com " + D1.trainInstances.numInstances() + " instâncias.");
        D1.trainClassifiers(printTrain);

        /* Evaluation Phase */
        D1.evaluateClassifiersPerCluster(printEvaluation, showProgress);

        /* Test Phase */
        D1.clusterAndTestSample(advices, true, true, printEvaluation, showProgress, features, AdviceEnum.ADVICE);

//        int VP = 0;
//        int VN = 0;
//        int FP = 0;
//        int FN = 0;
        for (DetectorClusterService d : D1.getClusters()) {
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
                            + "/" + D1.getCountTestInstances() + ")");
                    /* Atualiza Totais*/
                }

            }

        }
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Test Summary: [Solucionados "+ D1.getGoodAdvices()+"/"+ D1.getConflitos() + " conflitos de " + (D1.getVP() + D1.getVN() + D1.getFP() + D1.getFN()) + " classificações.] \n "
                + "VP	VN	FP	FN	F1Score \n"
                + D1.getVP() + ";" + D1.getVN() + ";" + D1.getFP() + ";" + D1.getFN() + ";" + String.valueOf(D1.getDetectionF1Score()).replace(".", ","));
        System.out.println("------------------------------------------------------------------------");

        return D1;
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
