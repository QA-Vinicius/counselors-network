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
}