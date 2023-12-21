package br.com.ids.service;

import br.com.ids.domain.DetectorClassifier;
import org.springframework.stereotype.Component;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

@Component
public class ClassifierService {

    private DetectorClassifier detectorClassifier;

    private Classifier classifier;

    private int VP, VN, FP, FN;

    long testNanotime = 0;

    long trainNanotime = 0;

    String normalClass;

    public double getTestAccuracy(DetectorClassifier detectorClassifier) {
        try {
            return Float.valueOf(
                    Float.valueOf((detectorClassifier.getVP() + detectorClassifier.getVN()) * 100)
                            / Float.valueOf(detectorClassifier.getVP() + detectorClassifier.getVN() + detectorClassifier.getFP() + detectorClassifier.getFN()));
        } catch (ArithmeticException e) {
            System.out.println(e.getLocalizedMessage());
        }
        return -1;
    }

    public double getTestF1Score(DetectorClassifier detectorClassifier) {
        try {
            double recall = (float) ((detectorClassifier.getVP() * 100) / (detectorClassifier.getVP() + detectorClassifier.getFN()));
            double precision = (float) ((detectorClassifier.getVP() * 100) / (detectorClassifier.getVP() + detectorClassifier.getFP()));
            return (float) (2 * (recall * precision) / (recall + precision));
        } catch (ArithmeticException e) {
            System.out.println(e.getLocalizedMessage());
        }
        return -1;
    }

    public Classifier train(Instances dataTrain, boolean showTrainingTime, DetectorClassifier detectorClassifier ) throws Exception {
        long currentTime = System.nanoTime();
        classifier.buildClassifier(dataTrain);
        long traininigTime = System.nanoTime() - currentTime;
        detectorClassifier.setTrainNanotime(traininigTime);
        if (showTrainingTime) {
            System.out.println("Classificador " + detectorClassifier.getName() + " treinado em: " + traininigTime / 1000000 + "ms/" + traininigTime + "ns");
        }
        return classifier;
    }

    private double classify(Instance singleInstance) throws Exception {
//        System.out.println(getName() + " classificando: " + singleInstance);
        return this.classifier.classifyInstance(singleInstance);
    }

    public Classifier resetAndEvaluate(Instances dataTest, ArrayList<Integer> clusteredInstances, DetectorClassifier detectorClassifier) throws Exception {
        long currentTime = System.nanoTime();
        detectorClassifier.setVN(0);
        detectorClassifier.setVP(0);
        detectorClassifier.setFN(0);
        detectorClassifier.setFP(0);
        detectorClassifier.setSelected(false);
        dataTest.setClassIndex(dataTest.numAttributes() - 1);
        for (int index = 0; index < clusteredInstances.size(); index++) {
            Instance instance = dataTest.get(index);
            if (classify(instance) == instance.classValue()) {
                if (instance.stringValue(instance.attribute(instance.classIndex())).equals(detectorClassifier.getNormalClass())) {
                    VN = VN + 1;
                } else {
                    VP = VP + 1;
                }
            } else {
                if (instance.stringValue(instance.attribute(instance.classIndex())).equals(detectorClassifier.getNormalClass())) {
                    FP = FP + 1;
                } else {
                    FN = FN + 1;
                }
            }
        }
        long evaluationTime = System.nanoTime() - currentTime;
        double recall = (float) ((detectorClassifier.getVP() * 100) / (detectorClassifier.getVP() + detectorClassifier.getFN()));
        double precision = (float) ((detectorClassifier.getVP() * 100) / (detectorClassifier.getVP() + detectorClassifier.getFP()));
        double f1Score = (float) (2 * (recall * precision) / (recall + precision));
        double accuracy = Float.valueOf(
                Float.valueOf((detectorClassifier.getVP() + detectorClassifier.getVN()) * 100)
                        / Float.valueOf(detectorClassifier.getVP() + detectorClassifier.getVN() + detectorClassifier.getFP() + detectorClassifier.getFN()));
        detectorClassifier.setEvaluationNanotime(evaluationTime);
        detectorClassifier.setEvaluationAccuracy(accuracy);
        detectorClassifier.setEvaluationF1Score(f1Score);
        return classifier;
    }

    public void resetConters(DetectorClassifier detectorClassifier) {
        detectorClassifier.setVN(0);
        detectorClassifier.setVP(0);
        detectorClassifier.setFN(0);
        detectorClassifier.setFP(0);
    }

    public double testSingle(Instance instance, DetectorClassifier detectorClassifier) throws Exception {
        long currentTime = System.nanoTime();
        double result = classify(instance);
        testNanotime = testNanotime + (currentTime - System.nanoTime());
        if (result == instance.classValue()) {
            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                VN = VN + 1;
            } else {
                VP = VP + 1;
            }
        } else {
            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                FP = FP + 1;
            } else {
                FN = FN + 1;
            }
        }
        return result;
    }
}