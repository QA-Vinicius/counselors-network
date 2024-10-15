package br.com.ids.service;

import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Arrays;

@Component
public class SampleProcessor {

    private final DetectorProcessor detectorProcessor;

    @Autowired
    public SampleProcessor(DetectorProcessor detectorProcessor) {
        this.detectorProcessor = detectorProcessor;
    }

    public void processSample(ConselorsDTO request, Detector detector) throws Exception {
        System.out.println("\tAction: Generate Advice\n");
        detector.onAdviceRequest(request);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO, Detector detector) throws Exception {
        System.out.println("\tAction: Learn With Advice\n");
        if (detector == null) {
            throw new IllegalStateException("\t[ERROR] Detector is not initialized.");
        }

        Instances trainInstances = detector.getTrainInstances();
        if (trainInstances.classIndex() == -1) {
            trainInstances.setClassIndex(trainInstances.numAttributes() - 1);
        }

        System.out.println("\tSended by: " + conselorsDTO.getId_conselheiro());

        System.out.println("\t1- Extracting the received sample and label");
        double[] sample = conselorsDTO.getSample();
        double sampleLabel = conselorsDTO.getResult();
        System.out.println("\t\t- Label: " + sampleLabel);

        double[] values = Arrays.copyOf(sample, sample.length + 1); // Adiciona espaço para o atributo de classe
        values[values.length - 1] = sampleLabel; // Valor inicial para o atributo de classe

        System.out.println("\t2- Creating new instance with labeled sample");
        Instance newInstance = new DenseInstance(1.0, values);
        newInstance.setDataset(trainInstances);

        System.out.println("\t3- Adding instance to trainInstances");
        trainInstances.add(newInstance);

//        System.out.println("TAMANHO INSTANCIA DE TREINO: " + trainInstances.numAttributes());

        System.out.println("\t4- Retraining the classifiers");
        detector = detectorProcessor.trainingStage(detector, false);
        detector = detectorProcessor.evaluationStage(detector, false, true);
    }
}
