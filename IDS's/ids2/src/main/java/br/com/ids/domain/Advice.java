/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.ids.domain;

import br.com.ids.data.DataSaver;
import br.com.ids.dto.ConselorsDTO;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Component;
import weka.core.Instance;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author vinicius
 */
@Component
public class Advice {

    double accuracy;
    double advisorResult;
    double correctResult;
    static String normalClass = "normal";
    public static int VP = 0, VN = 0, FP = 0, FN = 0;
    public static int correctsAdvicesIDS1 = 0;
    public static int correctsAdvicesIDS2 = 0;

    static DataSaver dataSaver = new DataSaver();

    // Iniciando o MAP que incrementara os conselhos corretos para cada classificador
    public static Map<String, Integer> correctAdvice = new HashMap<>(Map.ofEntries(
            Map.entry("1", 0),
            Map.entry("3", 0)
    ));


    public double getAccuracy() {
        return accuracy;
    }

    public double getAdvisorResult() {
        return advisorResult;
    }

    public double getCorrectResult() {
        return correctResult;
    }

    public String getNormalClass() {
        return normalClass;
    }

    public static void resetConters() {
        VN = 0;
        VP = 0;
        FN = 0;
        FP = 0;
    }

    public static double calculateTestF1Score() {
        try {
            double recall = (float) ((VP * 100) / (VP + FN));
            double precision = (float) ((VP * 100) / (VP + FP));
            return (float) (2 * (recall * precision) / (recall + precision));
        } catch (ArithmeticException e) {
//            System.out.println(e.getLocalizedMessage());
        }
        return -1;
    }

    public static double calculateTestAccuracy() {
        try {
            return Float.valueOf(
                    Float.valueOf((VP + VN) * 100)
                            / Float.valueOf(VP + VN + FP + FN));
        } catch (ArithmeticException e) {
//            System.out.println(e.getLocalizedMessage());
        }
        return -1;
    }

    public static String calculateAdviceMetrics(ConselorsDTO advice) throws IOException {
        Instance instance = Detector.testInstances.get(advice.getId_sample());

        String metric = null;
        if (advice.getResult() == instance.classValue()) {
            correctAdvice.put(advice.getId_conselheiro(), correctAdvice.get(advice.getId_conselheiro())+1);

            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                VN = VN + 1;
                metric = "VN";
            } else {
                VP = VP + 1;
                metric = "VP";
            }
        } else {
            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                FP = FP + 1;
                metric =  "FP";
            } else {
                FN = FN + 1;
                metric =  "FN";
            }
        }

        dataSaver.buildAdvicesMetricsCSV("advicesMetrics.csv", advice.getId_sample(), advice.getResult(), instance.classValue(), metric, advice.getId_conselheiro());

        return metric;
    }

    public static void calculateMetrics(String step, double result, Instance instance, int instanceIndex) throws IOException {
        String metric = null;

        if (result == instance.classValue()) {
            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                VN = VN + 1;
                metric = "VN";
            } else {
                VP = VP + 1;
                metric = "VP";
            }
        } else {
            if (instance.stringValue(instance.attribute(instance.classIndex())).equals(normalClass)) {
                FP = FP + 1;
                metric = "FP";
            } else {
                FN = FN + 1;
                metric = "FN";
            }
        }

        if(step.equals("Retest")) {
            dataSaver.buildRetestMetricsCSV("retestMetrics.csv", instanceIndex, result, instance.classValue(), metric);
        } else {
            dataSaver.buildTestMetricsCSV("testMetrics.csv", instanceIndex, result, instance.classValue(), metric);
        }
    }

    public static float porcentageCorrectAdvices(int advices, int correctsAdvices) {
        try {
            return Float.valueOf(((float)correctsAdvices / (float) advices) * 100);
        } catch (ArithmeticException e) {
//            System.out.println(e);
        }
        return -1;
    }
}
