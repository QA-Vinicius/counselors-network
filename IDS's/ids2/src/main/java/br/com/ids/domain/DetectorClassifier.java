/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.ids.domain;

import lombok.Getter;
import lombok.Setter;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

import java.util.ArrayList;

/**
 *
 * @author silvio
 */
@Getter
@Setter
public class DetectorClassifier {

    Classifier classifier;
    String name;
    double evaluationAccuracy;
    private double[] f1Scores;
    double evaluationF1Score;
    double testAccuracy;
    int VP, VN, FP, FN;
    long evaluationNanotime = 0;
    long testNanotime = 0;
    long trainNanotime = 0;
    boolean selected;
    String normalClass;

    public DetectorClassifier(Classifier classifier, String name, String normalClass) {
        this.classifier = classifier;
        this.name = name;
        this.normalClass = normalClass;
    }

    public double classify(Instance singleInstance) throws Exception {
//        System.out.println(getName() + " classificando: " + singleInstance);
        return this.classifier.classifyInstance(singleInstance);
    }

    public void resetConters() {
        setVN(0);
        setVP(0);
        setFN(0);
        setFP(0);
    }

    public Classifier train(Instances dataTrain, boolean showTrainingTime) throws Exception {
        long currentTime = System.nanoTime();
        classifier.buildClassifier(dataTrain);
        long traininigTime = System.nanoTime() - currentTime;
        setTrainNanotime(traininigTime);
        if (showTrainingTime) {
            System.out.println("Classificador " + getName() + " treinado em: " + traininigTime / 1000000 + "ms/" + traininigTime + "ns");
        }
        return classifier;
    }

    public Classifier resetAndEvaluate(Instances dataTest, ArrayList<Integer> clusteredInstances) throws Exception {
        //Inicializando vetor de f1-score com o numero de classes (vinicius)
        f1Scores = new double[dataTest.numClasses()];

        long currentTime = System.nanoTime();
        setVN(0);
        setVP(0);
        setFN(0);
        setFP(0);
        setSelected(false);
        dataTest.setClassIndex(dataTest.numAttributes() - 1);
        for (int index = 0; index < clusteredInstances.size(); index++) {
            Instance instance = dataTest.get(index);
            if (classify(instance) == instance.classValue()) {
                if (instance.stringValue(instance.attribute(instance.classIndex())).equals(getNormalClass())) {
                    VN = VN + 1;
                } else {
                    VP = VP + 1;
                }
            } else {
                if (instance.stringValue(instance.attribute(instance.classIndex())).equals(getNormalClass())) {
                    FP = FP + 1;
                } else {
                    FN = FN + 1;
                }
            }

            long evaluationTime = System.nanoTime() - currentTime;
            double recall = (float) ((getVP() * 100) / (getVP() + getFN()));
            double precision = (float) ((getVP() * 100) / (getVP() + getFP()));
            double f1Score = (float) (2 * (recall * precision) / (recall + precision));
            double accuracy = Float.valueOf(
                    Float.valueOf((getVP() + getVN()) * 100)
                            / Float.valueOf(getVP() + getVN() + getFP() + getFN()));
            setEvaluationNanotime(evaluationTime);
            setEvaluationAccuracy(accuracy);
            setEvaluationF1Score(f1Score);

            // Armazene o F1-score da classe atual no vetor (vinicius)
            int classIndex = (int) instance.classValue();
            f1Scores[classIndex] = f1Score;
        }

        return classifier;
    }

    public double testSingle(Instance instance) throws Exception {
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