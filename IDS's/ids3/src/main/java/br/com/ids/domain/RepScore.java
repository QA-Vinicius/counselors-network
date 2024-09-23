/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package br.com.ids.domain;

/**
 * @author vinicius
 */
public class RepScore {

    double repScore;

    public RepScore(double repScore) {
        this.repScore = repScore;
    }

    public double getRepScore() {
        return repScore;
    }

    public void setRepScore(double repScore) {
        this.repScore = repScore;
    }

    public void improveRepScore(double f1Score, Detector detector) {

    }

    public void decreaseRepScore(double f1Score, Detector detector) {

    }
}
