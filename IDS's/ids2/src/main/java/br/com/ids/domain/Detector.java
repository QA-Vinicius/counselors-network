package br.com.ids.domain;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.enuns.AdviceEnum;
import br.com.ids.producer.KafkaAdviceProducer;
import br.com.ids.producer.KafkaFeedbackProducer;
import br.com.ids.service.ClassifierService;
import br.com.ids.service.DetectorClusterService;
import br.com.ids.data.DataLoader;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import weka.clusterers.SimpleKMeans;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author vinicius
 */
public class Detector {

    // Variaveis para construcao do JSON a ser publicado
    private final KafkaAdviceProducer kafkaAdviceProducer;
    private final KafkaFeedbackProducer kafkaFeedbackProducer;
    private final String detectorID = "2"; // id de cada conselheiro

    DataLoader dataLoader = new DataLoader();

    SimpleKMeans kmeans;

    @Autowired
    DetectorClusterService[] clusters;
    public Instances trainInstances;
    Instances evaluationInstances;
    Instances evaluationInstancesNoLabel;
    Instances testInstances;
    Instances testInstancesNoLabel;
    int VP, VN, FP, FN;
    int conflitos = 0;
    int goodAdvices = 0;
    String normalClass;
    boolean saveTrainInsance = true; // Ponteiro para dividir entre treino e validação
    ArrayList<Advice> historicalData = new ArrayList<>();
    String strTestAcc = "";

    // Variaveis para receber as metricas de avaliacao
    List<Double> initialEvaluationAccuracies = new ArrayList<>();
    List<Double> initialEvaluationF1Scores = new ArrayList<>();
    Double initialEvaluationAverageAccuracy;
    Double initialEvaluationAverageF1Score;

    List<Double> finalEvaluationAccuracies = new ArrayList<>();
    List<Double> finalEvaluationF1Scores = new ArrayList<>();
    Double finalEvaluationAverageAccuracy;
    Double finalEvaluationAverageF1Score;

    // Variaveis para receber as metricas de teste
    List<Double> initialTestAccuracies = new ArrayList<>();
    List<Double> initialTestF1Scores = new ArrayList<>();
    Double initialTestAverageAccuracy;
    Double initialTestAverageF1Score;

    // Variavel que ira receber a relacao do f1Score e acuracia antes e depois do conselho, para validar se agregou ou nao
    Double deltaF1Score;
    Double deltaAccuracy;

    // Variavel que ira receber as classes abstraidas do CSV pelo metodo loadClassValues
    public static Map<Double, String> classValueMap = new HashMap<>();

    @Autowired(required = true)
    ClassifierService classifierService;

    public Detector(KafkaAdviceProducer kafkaAdviceProducer, KafkaFeedbackProducer kafkaFeedbackProducer, Instances trainInstances, Instances evaluationInstances, Instances testInstances, String normalClass) {
        this.trainInstances = trainInstances;
        this.evaluationInstances = evaluationInstances;
        this.evaluationInstancesNoLabel = new Instances(evaluationInstances);
        evaluationInstancesNoLabel.deleteAttributeAt(evaluationInstancesNoLabel.numAttributes() - 1);
        this.testInstances = testInstances;
        this.testInstancesNoLabel = new Instances(testInstances);
        testInstancesNoLabel.deleteAttributeAt(testInstancesNoLabel.numAttributes() - 1);
        this.testInstances.setClassIndex(evaluationInstances.numAttributes() - 1);
        this.normalClass = normalClass;
        this.kafkaAdviceProducer = kafkaAdviceProducer;
        this.kafkaFeedbackProducer = kafkaFeedbackProducer;
    }

    public void createClusters(int k, int seed) throws Exception {
        clusters = new DetectorClusterService[k];
        kmeans = new SimpleKMeans();
        kmeans.setSeed(seed);
        kmeans.setPreserveInstancesOrder(true);
        kmeans.setNumClusters(k);
//        System.out.println("DEBUG EVALUATION NO LABEL: " + evaluationInstancesNoLabel.lastInstance().numAttributes());
        kmeans.buildClusterer(evaluationInstancesNoLabel);

        for (int ki = 0; ki < k; ki++) {
            clusters[ki] = new DetectorClusterService(ki);
        }
        int[] assignments = kmeans.getAssignments(); // Avaliação No-Label
        for (int i = 0; i < assignments.length; i++) {
            int cluster = assignments[i];
            clusters[cluster].addInstanceIndex(i);
        }
    }

    //Metodo para abstrair do CSV as classes e mapear seus nomes para comparar com o double
    //Ex: 0.0 = Normal, 1.0 = random_replay ...
    public void loadClassValues(String fileName) throws Exception {
        Instances instances = dataLoader.leadAndFilter(false, fileName, new int[]{}); // Carregue as instâncias
        int classIndex = instances.classIndex();

        if (classIndex == -1) {
            classIndex = instances.numAttributes() - 1; // Padrão para o último atributo
            instances.setClassIndex(classIndex);
        }

        for (int i = 0; i < instances.numClasses(); i++) {
            double classValue = instances.classAttribute().indexOfValue(instances.classAttribute().value(i));
            String className = instances.classAttribute().value(i);
            classValueMap.put(classValue, className);
        }
    }

    public void trainClassifiers(boolean showTrainingTime) throws Exception {
        for (DetectorClusterService cluster : clusters) {
            cluster.trainClassifiers(trainInstances, showTrainingTime);
        }
    }

    public void addInstance(Instance instance) {
        trainInstances.add(instance);
    }

    public Instances getTrainInstances() {
        return trainInstances;
    }

    public Instances getEvaluationInstances() {
        return evaluationInstances;
    }

//    public void evaluateClassifiersPerCluster(boolean printEvaluation, boolean showProgress) throws Exception {
//        for (DetectorClusterService cluster : clusters) {
//            evaluationInstances.setClassIndex(evaluationInstances.numAttributes() - 1);
//            cluster.evaluateClassifiers(evaluationInstances);
//        }
//        selectClassifierPerCluster(showProgress);
//        if (printEvaluation) {
//            printEvaluationResults();
//        }
//    }

    public void evaluateClassifiersPerCluster(String stage, boolean printEvaluation, boolean showProgress) throws Exception {
        double totalAccuracy = 0;
        double totalF1Score = 0;
        int classifierCount = 0;

        System.out.print("\t--> Stage: " + stage);
        for (DetectorClusterService cluster : clusters) {
            evaluationInstances.setClassIndex(evaluationInstances.numAttributes() - 1);
            cluster.evaluateClassifiers(evaluationInstances);

            System.out.println("\n");
            // Obter as métricas de cada classificador e acumular
            for (DetectorClassifier c : cluster.getClassifiers()) {
                double accuracy = c.getEvaluationAccuracy();
                double f1Score = c.getEvaluationF1Score();

                totalAccuracy += accuracy;
                totalF1Score += f1Score;
                classifierCount++;

                System.out.println("\t\tClassifier " + c.getName() +
                        ": \t\tAccuracy: " + accuracy +
                        " | F1-Score: " + f1Score);

                if ("Evaluation Stage - Before Advice".equals(stage)) {
                    initialEvaluationAccuracies.add(accuracy);
                    initialEvaluationF1Scores.add(f1Score);
                }
                else if ("Evaluation Stage - After Advice".equals(stage)) {
                    finalEvaluationAccuracies.add(accuracy);
                    finalEvaluationF1Scores.add(f1Score);
                }
                else if ("Testing Stage".equals(stage)) {
                    initialTestAccuracies.add(accuracy);
                    initialTestF1Scores.add(f1Score);
                }
            }
        }

        if (classifierCount > 0) {
            double averageAccuracy = totalAccuracy / classifierCount;
            double averageF1Score = totalF1Score / classifierCount;

            if ("Evaluation Stage - Before Advice".equals(stage)) {
                initialEvaluationAverageAccuracy = averageAccuracy;
                initialEvaluationAverageF1Score = averageF1Score;
                System.out.println("\n\t\t- Average Accuracy (" + stage + "): " + initialEvaluationAverageAccuracy);
                System.out.println("\t\t- Average F1Score (" + stage + "): " + initialEvaluationAverageF1Score + "\n");
            }
            else if ("Evaluation Stage - After Advice".equals(stage)) {
                finalEvaluationAverageAccuracy = averageAccuracy;
                finalEvaluationAverageF1Score = averageF1Score;
                System.out.println("\n\t\t- Average Accuracy (" + stage + "): " + finalEvaluationAverageAccuracy);
                System.out.println("\t\t- Average F1Score (" + stage + "): " + finalEvaluationAverageF1Score + "\n");
            }
            else if ("Testing Stage".equals(stage)) {
                initialTestAverageAccuracy = averageAccuracy;
                initialTestAverageF1Score = averageF1Score;
                System.out.println("\n\t\t- Average Accuracy (" + stage + "): " + initialTestAverageAccuracy);
                System.out.println("\t\t- Average F1Score (" + stage + "): " + initialTestAverageF1Score + "\n");
            }
        }

        selectClassifierPerCluster(showProgress);

        if (printEvaluation) {
            printEvaluationResults();
        }
    }

    public void clusterAndTestSample(String stage, boolean enableAdvice, boolean learnWithAdvice, boolean learnWithoutAdvices, boolean printEvaResults, boolean showProgress, int[] features, AdviceEnum adviceEnum) throws Exception {
        // Calculando teste
//        int maxSizeTrain = 10000;
        boolean csv = true;
        int fimTrafegoNormal = -1;
        int percentRetrofeedAlfa = 10; //num of segments
        int percentRetrofeed = testInstances.size() / percentRetrofeedAlfa;
        int nextPoint = percentRetrofeed;
//        System.out.println("Ten percent: " + percentRetrofeed);

        for (int instIndex = 0; instIndex < testInstances.size(); instIndex++) {
            // System.out.println("#############");
            // System.out.println("##  Testando - " + instIndex + "/" + testInstances.size());
            // System.out.println("#############");
            /* Instância Atual */
            Instance instance = testInstances.get(instIndex);
            double correctValue = instance.classValue();

            if (!instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                if (fimTrafegoNormal == -1) {
                    fimTrafegoNormal = instIndex;
                }
            }

            if (nextPoint == instIndex) {
                nextPoint = nextPoint + percentRetrofeed;
                strTestAcc = strTestAcc + getDetectionAccuracyString() + ";";
//                System.out.println("\n\n\n\nAcc;" + strTestAcc);// + ";" + trainInstances.size() + ";" + evaluationInstances.size());
                if (learnWithoutAdvices) {
                    trainClassifiers(false);
                    evaluateClassifiersPerCluster(stage, printEvaResults, showProgress);
                }

                //Print to validate the best accuracies obtained from the cluster for each sample in it
                //for (DetectorClusterService cluster : clusters) {
                //    cluster.printStrEvaluation();
                //}
            }

            Instance evaluatingPeer = testInstancesNoLabel.get(instIndex);
            double[] sample = evaluatingPeer.toDoubleArray();
            int clusterNum = kmeans.clusterInstance(evaluatingPeer);
            ArrayList<DetectorClassifier> selectedClassifiers = clusters[clusterNum].getSelectedClassifiers();
            int qtdClassificadores = selectedClassifiers.size();
            double classifiersOutput[][] = new double[qtdClassificadores][testInstances.size()];

            /* Se o classificador for selecionado, classificar com ele */
            if (qtdClassificadores == 0) {
                double adviceResult = handleConflict(enableAdvice, correctValue, instIndex, instance, learnWithAdvice, evaluatingPeer, printEvaResults, showProgress, features, adviceEnum);
                System.out.println("[No Classifiers] Conflito n" + conflitos + " na instância " + instIndex + ", conselho: " + adviceResult + " / correto: " + correctValue);
            } else {
                for (int classifIndex = 0; classifIndex < qtdClassificadores; classifIndex++) {
                    DetectorClassifier c = selectedClassifiers.get(classifIndex);

                    double result = c.testSingle(instance);
                    classifiersOutput[classifIndex][instIndex] = result;

                    // Se nao for o primeiro classificador, mas houverem mais de um selecionado
                    if (classifIndex > 0 && classifIndex < qtdClassificadores - 1 && selectedClassifiers.size() > 1) {
                        // Checa conflito com o anterior
                        if (classifiersOutput[classifIndex][instIndex] != classifiersOutput[classifIndex - 1][instIndex]) {
                            ConselorsDTO conselorsDTO = ConselorsDTO.builder()
                                .id_conselheiro(detectorID)
                                .id_sample(instIndex)
                                .flag(adviceEnum.toString()) //identifica a msg como conselho ou pedido
                                .sample(sample)//(Arrays.stream(evaluatingPeer.toDoubleArray()).toArray())
                                .f1score(c.getEvaluationF1Score())
                                .timestamp(System.currentTimeMillis())
                                .build();

                            // Converta o objeto ConselorsDTO para JSON
                            ObjectMapper mapper = new ObjectMapper();
//                            String jsonMessage = mapper.writeValueAsString(conselorsDTO); RETIRADO POR ENQUANTO
                            //kafkaTemplate.send("topic name",jsonMessage);
                            // Enviar a mensagem JSON para o tópico do Kafka usando o kafkaTemplate
                            kafkaAdviceProducer.send(conselorsDTO);

                            double adviceResult = handleConflict(enableAdvice, correctValue, instIndex, instance, learnWithAdvice, evaluatingPeer, printEvaResults, showProgress, features, adviceEnum);
//                            System.out.println("[Divergence Classifiers] Conflito n" + conflitos + " na instância " + instIndex + ", conselho: " + adviceResult + " / correto: " + correctValue);
                        }
                        /* Se esse for o ultimo classificador da lista, é porque nao ocorreram conflitos*/
                    } else if (classifIndex == (qtdClassificadores - 1)) {
                        /* Salva um conselho para ser oferecido a outro detector */
                        historicalData.add(instIndex, new Advice(c.evaluationAccuracy, result, correctValue, normalClass));
                        updateResults(result, correctValue, instance);
                        /* Aprende sem Conflitos*/
                        if (learnWithoutAdvices) {
                            if (saveTrainInsance) {
                                trainInstances.add(instance); // Realimenta a cada amostra testada sem conflitos
                                saveTrainInsance = false;
//                            System.out.println("Aprendeu com conflito [" + conflitos + "].");
                            } else {
                                evaluationInstancesNoLabel.add(evaluatingPeer); // Realimenta a cada amostra testada sem conflitos
                                evaluationInstances.add(instance);
                                saveTrainInsance = true;
//                            System.out.println("Aprendeu com conflito.");
                            }
                        }
                    }
                }
            }
        }

//        System.out.println("Good Advices: " + getGoodAdvices() + "/" + conflitos);
//        System.out.println("Os ataques começaram na amostra " + fimTrafegoNormal + "(" + (fimTrafegoNormal / testInstances.numAttributes()) + "%)");
    }

    public Advice getAdvice(int timestamp) {
        return historicalData.get(timestamp);
    }

    public DetectorClusterService[] getClusters() {
        return clusters;
    }

    public void setClusters(DetectorClusterService[] clusters) {
        this.clusters = clusters;
    }

    public void selectClassifierPerCluster(boolean showProgress) throws Exception {
        for (DetectorClusterService cluster : clusters) {
            cluster.classifierSelection(showProgress);
        }
    }

    public int getConflitos() {
        return conflitos;
    }

    public void resetConters() {
        for (DetectorClusterService cluster : clusters) {
            for (DetectorClassifier classifier : cluster.getClassifiers()) {
                classifier.resetConters();
            }
        }

        setVN(0);
        setVP(0);
        setFN(0);
        setFP(0);
        conflitos = 0;
    }

    public int getVP() {
        return VP;
    }

    public void setVP(int VP) {
        this.VP = VP;
    }

    public int getVN() {
        return VN;
    }

    public void setVN(int VN) {
        this.VN = VN;
    }

    public int getFP() {
        return FP;
    }

    public void setFP(int FP) {
        this.FP = FP;
    }

    public int getFN() {
        return FN;
    }

    public void setFN(int FN) {
        this.FN = FN;
    }

    public double getDetectionAccuracy() {
        try {
            return Float.valueOf(
                    Float.valueOf((getVP() + getVN()) * 100)
                    / Float.valueOf(getVP() + getVN() + getFP() + getFN()));
        } catch (ArithmeticException e) {
//            System.out.println(e.getLocalizedMessage());
        }
        return -1;
    }

    public String getDetectionAccuracyString() {
        try {
            double acc = Float.valueOf(
                    Float.valueOf((getVP() + getVN()) * 100)
                    / Float.valueOf(getVP() + getVN() + getFP() + getFN()));
            return String.valueOf(acc / 100).replace(".", ",");
        } catch (ArithmeticException e) {
//            System.out.println(e.getLocalizedMessage());
        }
        return "-1";
    }

    public double getDetectionF1Score() {
        try {
            double recall = (float) ((getVP() * 100) / (getVP() + getFN()));
            double precision = (float) ((getVP() * 100) / (getVP() + getFP()));
            return (float) (2 * (recall * precision) / (recall + precision));
        } catch (ArithmeticException e) {
//            System.out.println(e.getLocalizedMessage());
        }
        return -1;
    }


    public int getCountTestInstances() {
        return testInstances.size();
    }

    public ArrayList<Advice> getHistoricalData() {
        return historicalData;
    }

    public int getGoodAdvices() {
        return goodAdvices;
    }

    private double handleConflict(boolean enableAdvice, double correctValue,
            int instIndex, Instance instance, boolean learnWithAdvice, Instance evaluatingPeer, boolean printEvaResu, boolean showProgress, int[] features, AdviceEnum adviceEnum) throws Exception {
        historicalData.add(new Advice(0, 6, correctValue, normalClass));
        conflitos = conflitos + 1;

        // flag para validar se ha ou nao detectores para o detector solicitante (continua 77 em caso de nao haver)
        double result = 6;
        /* REDE DE CONSELHOS */
        if (enableAdvice) {
//            System.out.println("########## CONFLITO #########");
//            System.out.println("Classifier output:" + classifiersOutput[classifIndex][instIndex] + " vs " + classifiersOutput[classifIndex - 1][instIndex]);

           //Consumer do RequestAdvice
                Advice advice = getAdvice(instIndex);
//                                    System.out.println("Requesting conseil...");
//                                    System.out.println("Advisors' response: " + advice.getAdvisorResult() + " (" + String.valueOf(advice.accuracy).substring(0, 5) + ")%, correct is: " + advice.correctResult + "(Good Adivices: " + getGoodAdvices() + ")");
                result = advice.getAdvisorResult();
                instance.setClassValue(result);
            if (learnWithAdvice) {
                trainInstances.add(instance); // Realimenta a cada conselho
                /* Treina Todos Classificadores Novamente */
                trainClassifiers(false);
//                evaluateClassifiersPerCluster("beforeAdvice", printEvaResu, showProgress);
                selectClassifierPerCluster(showProgress);
            }
            if (advice.getAdvisorResult() == correctValue) {
                goodAdvices = goodAdvices + 1;
                double[] teste = {1.0, 2.0, 3.0, 4.0};
                ConselorsDTO conselorsDTO = ConselorsDTO.builder()
                        .id_conselheiro(detectorID)
                        .flag(String.valueOf(AdviceEnum.FEEDBACK)) //diferencia o feedback de um conselho
                        .sample(teste)//(Arrays.stream(evaluatingPeer.toDoubleArray()).toArray()))
                        .timestamp(System.currentTimeMillis())
                        .feedback("Positive")
                        .build();

                // Converta o objeto ConselorsDTO para JSON
                ObjectMapper mapper = new ObjectMapper();

                kafkaFeedbackProducer.sendFeedback(conselorsDTO);

                System.out.println("FEEDBACK POSITIVO\n");
//                                        System.out.println("Class: " + advice.getClassNormal());
                if (advice.getClassNormal().equals(normalClass)) {
                    VN = VN + 1;
                } else {
                    VP = VP + 1;
                }
            } else {
                if (advice.getClassNormal().equals(normalClass)) {
                    FP = FP + 1;
                } else {
                    FN = FN + 1;
                }
            }

            if (learnWithAdvice) {
                if (saveTrainInsance) {
                    trainInstances.add(instance); // Realimenta a cada amostra testada sem conflitos
                    saveTrainInsance = false;
//                                            System.out.println("Aprendeu com conflito [" + conflitos + "].");
                } else {
                    evaluationInstances.add(instance);
                    evaluationInstancesNoLabel.add(evaluatingPeer); // Realimenta a cada amostra testada sem conflitos
                    saveTrainInsance = true;
//                                            System.out.println("Aprendeu com conflito [" + conflitos + "].");
                }
            }
        }
        return result;
    }

    private void updateResults(double result, double correctValue, Instance instance) {
        if (result == correctValue) {
            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                VN = VN + 1;
            } else {
                VP = VP + 1;
            }
        } else {
            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                FP = FP + 1;
                instance.setClassValue(result);
            } else {
                FN = FN + 1;
                instance.setClassValue(result);
            }
        }
    }

    public void printEvaluationResults() {
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Evaluation");
        System.out.println("------------------------------------------------------------------------");
        for (DetectorClusterService d : getClusters()) {
            System.out.println("\n---- Cluster " + d.getClusterNum() + ":");
            for (DetectorClassifier c : d.getClassifiers()) {
                if (c.isSelected()) {
                    System.out.println("[X]" + c.getName()
                            + " - " + c.getEvaluationF1Score() // antes era + " - " + c.getEvaluationAccuracy()
                            + " (VP;VN;FP;FN) = "
                            + "("
                            + c.getVP()
                            + ";" + c.getVN()
                            + ";" + c.getFP()
                            + ";" + c.getFN()
                            + ")"
                    );
                } else {
                    System.out.println(c.getName()
                            + "[N] - " + c.getEvaluationF1Score() // antes era + "[N] - " + c.getEvaluationAccuracy()
                            + " (VP;VN;FP;FN) = "
                            + "("
                            + c.getVP()
                            + ";" + c.getVN()
                            + ";" + c.getFP()
                            + ";" + c.getFN()
                            + ")"
                    );
                }
            }
        }
    }

    public void printTestResults() {
        for (DetectorClusterService d : getClusters()) {
            System.out.println("---- Cluster " + d.getClusterNum() + ":");
            for (DetectorClassifier c : d.getClassifiers()) {
                if (c.isSelected()) {
                    System.out.println("[X]" + c.getName()
                            + " - " + classifierService.getTestF1Score(c) // antes era + " - " + c.getTestAccuracy()
                            + " (VP;VN;FP;FN) = "
                            + "("
                            + c.getVP()
                            + ";" + c.getVN()
                            + ";" + c.getFP()
                            + ";" + c.getFN()
                            + ") = ("
                            + (c.getVP() + c.getVN() + c.getFP() + c.getFN())
                            + "/" + getCountTestInstances() + ")");
                }
            }
        }

        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Test Summary: [Solucionados " + getGoodAdvices() + "/" + getConflitos() + " conflitos de " + (getVP() + getVN() + getFP() + getFN()) + " classificações.] \n "
                + "VP	VN	FP	FN	F1Score \n"
                + getVP() + ";" + getVN() + ";" + getFP() + ";" + getFN() + ";" + String.valueOf(getDetectionF1Score()).replace(".", ","));
                // antes era + getVP() + ";" + getVN() + ";" + getFP() + ";" + getFN() + ";" + String.valueOf(getDetectionAccuracy()).replace(".", ","));
        System.out.println("------------------------------------------------------------------------");
    }

    //Generate Advice
    public void onAdviceRequest(ConselorsDTO request) throws Exception {
        double[] sample = request.getSample();

        // Associar instância ao dataset do detector
        Instances trainInstances = getTrainInstances();

        //System.out.println("TrainInstances numAttributes: " + trainInstances.numAttributes());
        if (trainInstances.classIndex() == -1) {
            trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
        }

        //System.out.println("\n-- DEBUG PARA COMPARAR NUMERO DE ATRIBUTOS");
        //System.out.println("\t- Tamanho da amostra: " + sample.length);

        System.out.println("\t1- Creating new instance with the received sample");
        Instance newInstance = new DenseInstance(1.0, sample);

        //System.out.println("\t- KMEANS:: " + kmeans.getClusterCentroids().get(0).numAttributes());
        //System.out.println("\t- newInstance:: " + newInstance.numAttributes());

        System.out.println("\t2- Identifying the corresponding cluster for this sample");
        int clusterNum = kmeans.clusterInstance(newInstance);
        DetectorClusterService cluster = clusters[clusterNum];

        System.out.println("\t3- Getting the selected classifiers from this cluster");
        ArrayList<DetectorClassifier> selectedClassifiers = cluster.getSelectedClassifiers();

        System.out.println("\t4- Associating the instance with the evaluation dataset");
        newInstance.setDataset(evaluationInstances); // Associa a instância ao dataset

        double bestResult = Double.NaN;
        double bestF1Score = -1;
        String predictedClassName = "";

        System.out.println("\t5- Getting better classification result");
        for (DetectorClassifier classifier : selectedClassifiers) {
            double result = classifier.classifyInstance(newInstance);
            //System.out.println("ACOMPANHA F1SCORE: " + classifier.getEvaluationF1Score());
            if (classifier.getEvaluationF1Score() > bestF1Score) {
                bestF1Score = classifier.getEvaluationF1Score();
                bestResult = result;
                predictedClassName = classValueMap.get(bestResult);
            }
        }

        System.out.println("\t\t|Best F1Score: " + bestF1Score);
        System.out.println("\t\t|Best Result: " + bestResult);
        System.out.println("\t\t|Class Name: " + predictedClassName);

        System.out.println("\t6- Sending Advice");
        ConselorsDTO conselorsDTO = ConselorsDTO.builder()
                .id_conselheiro(detectorID)
                .id_sample(request.getId_sample())
                .flag(String.valueOf(AdviceEnum.RESPONSE_ADVICE))
                .sample(sample)
                .f1score(bestF1Score)
                .result(bestResult)
                .timestamp(System.currentTimeMillis())
                .build();

        kafkaAdviceProducer.send(conselorsDTO);
    }

    public void sendFeedback(int id_sample, double[] sample, double result) {
        //Comparar a diferenca de f1Score e acuracia
        deltaF1Score = finalEvaluationAverageF1Score - initialEvaluationAverageF1Score;
        deltaAccuracy = finalEvaluationAverageAccuracy - initialTestAverageAccuracy;

        System.out.println("\t\t- Delta F1-Score: " + deltaF1Score);
        System.out.println("\t\t- Delta Accuracy: " + deltaAccuracy);

        String feedback = "";
        if(deltaF1Score > 0.0) {
            System.out.println("\t\t- Feedback positivo!\n");
            feedback = "Positive";
        } else {
            System.out.println("\t\t- Feedback negativo!\n");
            feedback = "Negative";
        }

        ConselorsDTO conselorsDTO = ConselorsDTO.builder()
                .id_conselheiro(detectorID)
                .id_sample(id_sample)
                .flag(String.valueOf(AdviceEnum.FEEDBACK))
                .feedback(feedback)
                .sample(sample)
                .result(result)
                .f1score(finalEvaluationAverageF1Score)
                .deltaF1Score(deltaF1Score)
                .timestamp(System.currentTimeMillis())
                .build();

        ObjectMapper mapper = new ObjectMapper();
        kafkaFeedbackProducer.sendFeedback(conselorsDTO);
    }
}
