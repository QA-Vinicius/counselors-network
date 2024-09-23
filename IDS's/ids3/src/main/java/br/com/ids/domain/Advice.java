/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.ids.domain;

/**
 * @author vinicius
 */
public class Advice {

    double accuracy;
    double advisorResult;
    double correctResult;
    String classNormal;

    public Advice(double accuracy, double advisorResult, double correctResult, String classNormal) {
        this.accuracy = accuracy;
        this.advisorResult = advisorResult;
        this.correctResult = correctResult;
        this.classNormal = classNormal;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public double getAdvisorResult() {
        return advisorResult;
    }

    public double getCorrectResult() {
        return correctResult;
    }

    public String getClassNormal() {
        return classNormal;
    }

}
