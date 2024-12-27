package br.com.ids.scheduling;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.trees.J48;
import weka.classifiers.Evaluation;

public class WekaJ48Example {

    public static void main(String[] args) {
        try {
            // Carregar o arquivo de treino
            DataSource trainSource = new DataSource("c2-train.arff");
            Instances trainData = trainSource.getDataSet();

            // Definir a última coluna como atributo de classe
            if (trainData.classIndex() == -1) {
                trainData.setClassIndex(trainData.numAttributes() - 1);
            }

            // Carregar o arquivo de teste
            DataSource testSource = new DataSource("c2-test.arff");
            Instances testData = testSource.getDataSet();

            // Definir a última coluna como atributo de classe
            if (testData.classIndex() == -1) {
                testData.setClassIndex(testData.numAttributes() - 1);
            }

            // Inicializar o classificador J48
            J48 tree = new J48();
            tree.buildClassifier(trainData);

            // Avaliar o modelo no conjunto de teste
            Evaluation eval = new Evaluation(trainData);
            eval.evaluateModel(tree, testData);

            // Calcular e imprimir a F1-Score para cada classe
            for (int i = 0; i < testData.numClasses(); i++) {
                double f1Score = eval.fMeasure(i);
                System.out.println("F1-Score para a classe " + testData.classAttribute().value(i) + ": " + f1Score);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}