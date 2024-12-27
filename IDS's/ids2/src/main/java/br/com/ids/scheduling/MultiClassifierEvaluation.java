package br.com.ids.scheduling;

import weka.core.Instances;
import weka.core.converters.ConverterUtils.DataSource;
import weka.classifiers.Classifier;
import weka.classifiers.trees.J48;
import weka.classifiers.trees.RandomTree;
import weka.classifiers.trees.RandomForest;
import weka.classifiers.trees.REPTree;
import weka.classifiers.bayes.NaiveBayes;
import weka.classifiers.Evaluation;

public class MultiClassifierEvaluation {

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

            // Lista de classificadores a serem avaliados
            Classifier[] classifiers = {
                    new J48(),
                    new RandomTree(),
                    new RandomForest(),
                    new REPTree(),
                    new NaiveBayes()
            };

            // Nomes dos classificadores para referência
            String[] classifierNames = {
                    "J48",
                    "Random Tree",
                    "Random Forest",
                    "REP Tree",
                    "Naive Bayes"
            };

            // Avaliar cada classificador
            for (int i = 0; i < classifiers.length; i++) {
                Classifier classifier = classifiers[i];
                String classifierName = classifierNames[i];

                // Treinar o classificador
                classifier.buildClassifier(trainData);

                // Avaliar o classificador no conjunto de teste
                Evaluation eval = new Evaluation(trainData);
                eval.evaluateModel(classifier, testData);

                // Exibir F1-Score para cada classe
                System.out.println("=== Avaliação do classificador: " + classifierName + " ===");
                for (int j = 0; j < testData.numClasses(); j++) {
                    double f1Score = eval.fMeasure(j);
                    System.out.println("F1-Score para a classe " + testData.classAttribute().value(j) + ": " + f1Score);
                }
                System.out.println();  // Separador entre os classificadores
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}