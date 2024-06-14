/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.ids.service;

import br.com.ids.domain.DetectorClassifier;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.REPTree;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.RandomTree;
import weka.core.Instances;

import java.util.ArrayList;

/**
 *
 * @author silvio
 */
@Component
@Getter
@Setter
public class DetectorClusterService {

    DetectorClassifier[] classifiers = {
        new DetectorClassifier(new RandomTree(), "Random Tree", "BENIGN"),
        new DetectorClassifier(new RandomForest(), "Random Forest", "BENIGN"),
        new DetectorClassifier(new NaiveBayes(), "Naive Bayes", "BENIGN"),
        new DetectorClassifier(new J48(), "J48", "BENIGN"),
        new DetectorClassifier(new REPTree(), "REP Tree", "BENIGN")
    };
    ArrayList<Integer> clusteredInstancesIndex; //[cluster][index]
    int clusterNum;

    double threshold = 3.0; // 2% do best
    double minAccAcceptable = 80.0;
    String strAcc = "";

    ArrayList<DetectorClassifier> selectedClassifiers;

    private ClassifierService classifierService;

    public DetectorClusterService(int clusterNum) {
        this.clusteredInstancesIndex = new ArrayList<Integer>();
        this.clusterNum = clusterNum;
    }

    public DetectorClusterService() {
    }
    public void addInstanceIndex(int index) {
        this.clusteredInstancesIndex.add(index);
    }

    public void evaluateClassifiers(Instances dataEvaluation) throws Exception {
        for (DetectorClassifier c : classifiers) {
            System.out.println("\nAvaliaçao do Classificador: " + c.getName());
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
            if (c.getEvaluationAccuracy() > best.getEvaluationAccuracy()) {
                best = c;
            }
        }

        for (DetectorClassifier c : classifiers) {
            if ((c.getEvaluationAccuracy() + threshold >= best.getEvaluationAccuracy()) && (c.getEvaluationAccuracy() >= getMinAccAcceptable())) {
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
}
