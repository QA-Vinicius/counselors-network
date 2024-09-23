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

}