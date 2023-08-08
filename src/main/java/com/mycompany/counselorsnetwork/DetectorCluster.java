/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.mycompany.counselorsnetwork;

import static com.mycompany.counselorsnetwork.Main.NORMAL_CLASS;
import java.util.ArrayList;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Instances;

/**
 *
 * @author silvio
 */
public class DetectorCluster {

    DetectorClassifier[] classifiers = {
        new DetectorClassifier(new RandomTree(), "Random Tree", NORMAL_CLASS),
        new DetectorClassifier(new RandomForest(), "Random Forest", NORMAL_CLASS),
        new DetectorClassifier(new NaiveBayes(), "Naive Bayes", NORMAL_CLASS),
        new DetectorClassifier(new J48(), "J48", NORMAL_CLASS),
        new DetectorClassifier(new REPTree(), "REP Tree", NORMAL_CLASS)
    };

    ArrayList<DetectorClassifier> selectedClassifiers;

    ArrayList<Integer> clusteredInstancesIndex; //[cluster][index]
    int clusterNum;
    double threshold = 3.0; // 2% do best 
    double minAccAcceptable = 80.0;
    String strAcc = "";

    public DetectorCluster(int clusterNum) {
        this.clusteredInstancesIndex = new ArrayList<Integer>();
        this.clusterNum = clusterNum;
    }

    public void addInstanceIndex(int index) {
        this.clusteredInstancesIndex.add(index);
    }

    public void evaluateClassifiers(Instances dataEvaluation) throws Exception {
        for (DetectorClassifier c : classifiers) {
            c.resetAndEvaluate(dataEvaluation, clusteredInstancesIndex);
        }
    }

    public void printStrEvaluation() {
        System.out.println("Cluster " + clusterNum + ";" + strAcc);
    }

    public void classifierSelection(boolean showProgressSelection) throws Exception {
        selectedClassifiers = new ArrayList<>();
        DetectorClassifier best = classifiers[0];
        for (DetectorClassifier c : classifiers) {
            if (c.evaluationAccuracy > best.getEvaluationAccuracy()) {
                best = c;
            }
        }

        for (DetectorClassifier c : classifiers) {
            if ((c.evaluationAccuracy + threshold >= best.getEvaluationAccuracy()) && (c.evaluationAccuracy >= getMinAccAcceptable())) {
                selectedClassifiers.add(c);
                c.setSelected(true);
//                System.out.println("Classificador: "+c.getName()+" selecionado."+c.evaluationAccuracy+" >= "+getMinAccAcceptable());
            } else {
                c.setSelected(false);
//                System.out.println("Classificador: "+c.getName()+" excluido."); 
            }
        }
        if (showProgressSelection) {
            strAcc = strAcc + String.valueOf(best.getEvaluationAccuracy()).replace(".", ",") + ";";
        }
    }

    public void trainClassifiers(Instances dataTrain, boolean showTrainingTime) throws Exception {
        for (DetectorClassifier c : classifiers) {
            dataTrain.setClassIndex(dataTrain.numAttributes() - 1);
            c.train(dataTrain, showTrainingTime);
        }
    }

    public ArrayList<DetectorClassifier> getSelectedClassifiers() {
        return this.selectedClassifiers;
    }

    public ArrayList<Integer> getClusteredInstancesIndex() {
        return clusteredInstancesIndex;
    }

    public int getClusterNum() {
        return clusterNum;
    }

    public void setClusterNum(int clusterNum) {
        this.clusterNum = clusterNum;
    }

    public double getThreshold() {
        return threshold;
    }

    public void setThreshold(double threshold) {
        this.threshold = threshold;
    }

    public DetectorClassifier[] getClassifiers() {
        return classifiers;
    }

    public double getMinAccAcceptable() {
        return minAccAcceptable;
    }

    public void setMinAccAcceptable(double minAccAcceptable) {
        this.minAccAcceptable = minAccAcceptable;
    }
}
