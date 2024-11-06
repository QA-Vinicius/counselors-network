package br.com.ids.service;

import br.com.ids.domain.Advice;
import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.metrics.TimeLogger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;

import static br.com.ids.domain.Detector.classValueMap;
import static br.com.ids.domain.Detector.formato_br;
import static br.com.ids.service.ConflictService.getConflitos;

@Component
public class SampleProcessor {

    private final DetectorProcessor detectorProcessor;

    // Variavel criada apenas para preencher parametros obrigatorios do testStage
    int[] noFeatureSelection = new int[]{};

    @Autowired
    public SampleProcessor(DetectorProcessor detectorProcessor) {
        this.detectorProcessor = detectorProcessor;
    }

    public void processSample(ConselorsDTO request, Detector detector) throws Exception {
        System.out.println("\tAction: Generate Advice\n");
        detector.onAdviceRequest(request);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO, Detector detector) throws Exception {
        TimeLogger.start("Learning with Sample " + conselorsDTO.getId_sample());

        System.out.println("\n\tAction: Learn With Advice");
        if (detector == null) {
            throw new IllegalStateException("\t[ERROR] Detector is not initialized.");
        }

        Instances trainInstances = detector.getTrainInstances();
        if (trainInstances.classIndex() == -1) {
            trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
        }

        Instances evaluateInstances = detector.getEvaluationInstances();
        if (evaluateInstances.classIndex() == -1) {
            evaluateInstances.setClassIndex(evaluateInstances.numAttributes() - 1);
        }

        System.out.println("\tSended by: Counselor " + conselorsDTO.getId_conselheiro());

        System.out.println("\t| - Extracting the received sample and label");
        double[] sample = conselorsDTO.getSample();
        double sampleLabel = conselorsDTO.getResult();
        System.out.println("\t|\t- Label: " + sampleLabel + " ("+ classValueMap.get(sampleLabel) + ")");

        System.out.println("\t| - Calculating metrics with this sample");
        String metricResult = Advice.calculateAdviceMetrics(conselorsDTO);
        System.out.println("\t|\t- Metric Result: " + metricResult);

        double[] values = Arrays.copyOf(sample, sample.length + 1); // Adiciona espaço para o atributo de classe
        values[values.length - 1] = sampleLabel; // Valor inicial para o atributo de classe

        System.out.println("\t| - Creating new instance with labeled sample (Train Instances)");
        Instance newTrainInstance = new DenseInstance(1.0, values);
        newTrainInstance.setDataset(trainInstances);

        System.out.println("\t| - Adding instance to trainInstances");
        trainInstances.add(newTrainInstance);

        System.out.println("\t| - Retraining and reevaluating the classifiers\n");
        TimeLogger.start("Training Stage - After Advice");
        detector = detectorProcessor.trainingStage(detector, false);
        TimeLogger.start("Training Stage - After Advice");

        detector = detectorProcessor.evaluationStage("Evaluation Stage - After Advice", detector, false, true);

        System.out.println("\t| - Comparing metrics and giving feedback");
        String feedback = detector.sendFeedback(conselorsDTO.getId_sample(), sample, sampleLabel);

        System.out.println("\t|\t- Good Advices (based on Evaluation Stage): " + detector.getGoodAdvices() + "/" + getConflitos());
        System.out.println("\t|\t- Bad Advices (based on Evaluation Stage): " + detector.getBadAdvices() + "/" + getConflitos());

//        if(feedback.equals("Negative")) {
//            System.out.print("\t\t-- Removing instance from dataset because feedback was negative!");
//            trainInstances.delete(trainInstances.numInstances() - 1); //indice da  ultima instancia adicionada
//
//            System.out.println(" (New trainInstances: " + trainInstances.size() + ")");
//        } else {
        System.out.println("\t| - Retesting!");
        detector.resetConters();
        detector = detectorProcessor.retestStage("Testing Stage - After each Advice", detector, conselorsDTO, false, true);

        TimeLogger.stop("Learning with Sample " + conselorsDTO.getId_sample());
        TimeLogger.stop("Learning Phase");

        System.out.println("\n\n\t-- Comparing Test Stage metrics");
        detector.compareTestMetrics(false, conselorsDTO.getId_sample());
//        }
    }

    public void analyzeFinalPerformance(ConselorsDTO conselorsDTO, Detector detector) throws Exception {
        System.out.println("\n\n------------------------------------------------------------------------");
        System.out.println("\n\n-- Analyzing the final performance of the detector");
        System.out.println("\n\n------------------------------------------------------------------------");

        System.out.println("-- Retraining the classifiers with new instances");
        TimeLogger.start("Training Stage - Final");
        detector = detectorProcessor.trainingStage(detector, false);
        TimeLogger.start("Training Stage - Final");

        System.out.println("-- Reevaluating the classifiers");
        detector = detectorProcessor.evaluationStage("Evaluation Stage - Final", detector, false, true);

        // Zera todas as variaveis para avaliação
        detector.resetConters();
        Advice.resetConters();

        System.out.println("-- Retesting to validate final performance");
        detector = detectorProcessor.testStage("Testing Stage - Final",  detector, true, false, true, noFeatureSelection);

        System.out.println("-- Comparing Test Stage metrics");
        detector.compareTestMetrics(true, null);
    }
}