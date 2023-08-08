/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.counselorsnetwork;

import java.util.ArrayList;
import weka.classifiers.Classifier;
import weka.core.Instance;
import weka.core.Instances;

/**
 *
 * @author silvio
 */
public class DetectorClassifier {

    Classifier classifier;
    String name;
    double evaluationAccuracy;

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
        return classifier;
    }

    public void resetConters() {
        setVN(0);
        setVP(0);
        setFN(0);
        setFP(0);
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

    private double classify(Instance singleInstance) throws Exception {
//        System.out.println(getName() + " classificando: " + singleInstance);
        return this.classifier.classifyInstance(singleInstance);
    }

    public Classifier getClassifier() {
        return classifier;
    }

    public void setClassifier(Classifier classifier) {
        this.classifier = classifier;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getEvaluationAccuracy() {
        return evaluationAccuracy;
    }

    public void setEvaluationAccuracy(double evaluationAccuracy) {
        this.evaluationAccuracy = evaluationAccuracy;
    }

    public double getEvaluationF1Score() {
        return evaluationF1Score;
    }

    public void setEvaluationF1Score(double evaluationF1Score) {
        this.evaluationF1Score = evaluationF1Score;
    }

    public double getTestAccuracy() {
        try {
            return Float.valueOf(
                    Float.valueOf((getVP() + getVN()) * 100)
                    / Float.valueOf(getVP() + getVN() + getFP() + getFN()));
        } catch (ArithmeticException e) {
//            System.out.println(e.getLocalizedMessage());
        }
        return -1;
    }

    public double getTestF1Score() {
        try {
            double recall = (float) ((getVP() * 100) / (getVP() + getFN()));
            double precision = (float) ((getVP() * 100) / (getVP() + getFP()));
            return (float) (2 * (recall * precision) / (recall + precision));
        } catch (ArithmeticException e) {
//            System.out.println(e.getLocalizedMessage());
        }
        return -1;
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

    public long getEvaluationNanotime() {
        return evaluationNanotime;
    }

    public void setEvaluationNanotime(long evaluationNanotime) {
        this.evaluationNanotime = evaluationNanotime;
    }

    public long getTestNanotime() {
        return testNanotime;
    }

    public void setTestNanotime(long testNanotime) {
        this.testNanotime = testNanotime;
    }

    public long getTrainNanotime() {
        return trainNanotime;
    }

    public void setTrainNanotime(long trainNanotime) {
        this.trainNanotime = trainNanotime;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }

    public String getNormalClass() {
        return normalClass;
    }
}
