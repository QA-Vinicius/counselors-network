package br.com.ids.service;

import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.util.Arrays;

import static br.com.ids.domain.Detector.classValueMap;

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

        System.out.println("\t1- Extracting the received sample and label");
        double[] sample = conselorsDTO.getSample();
        double sampleLabel = conselorsDTO.getResult();
        System.out.println("\t\t- Label: " + sampleLabel + " ("+ classValueMap.get(sampleLabel) + ")");

        double[] values = Arrays.copyOf(sample, sample.length + 1); // Adiciona espaço para o atributo de classe
        values[values.length - 1] = sampleLabel; // Valor inicial para o atributo de classe

        System.out.println("\t2- Creating new instance with labeled sample (Train Instances)");
        Instance newTrainInstance = new DenseInstance(1.0, values);
        newTrainInstance.setDataset(trainInstances);

        System.out.println("\t3- Adding instance to trainInstances");
        trainInstances.add(newTrainInstance);

//        System.out.println("TAMANHO INSTANCIA DE TREINO: " + trainInstances.numAttributes());

        System.out.println("\t4- Retraining the classifiers \n");
        detector = detectorProcessor.trainingStage(detector, false);
        detector = detectorProcessor.evaluationStage("Evaluation Stage - After Advice", detector, false, true);

        System.out.println("\t5- Comparing metrics and giving feedback");
        detector.sendFeedback(conselorsDTO.getId_sample(), sample, sampleLabel);

        System.out.println("\t\t- Good Advices: " + detector.getGoodAdvices() + "/" + detector.getConflitos());
        System.out.println("\t\t- Bad Advices: " + detector.getBadAdvices() + "/" + detector.getConflitos());
    }
}
