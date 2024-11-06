package br.com.ids.service;

import br.com.ids.data.DataSaver;
import br.com.ids.domain.Advice;
import br.com.ids.domain.Detector;
import br.com.ids.domain.DetectorClassifier;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.enuns.AdviceEnum;
import br.com.ids.metrics.TimeLogger;
import org.springframework.stereotype.Component;

import static br.com.ids.service.ConflictService.getConflitos;
import static br.com.ids.service.ConflictService.resetConflicts;

@Component
public class DetectorProcessor {

    DataSaver dataSaver = new DataSaver();
    private int indexConselho = 0; //variavel a ser incrementada a cada metodo retestStage que sera usada como id do conselho no csv

    /* Train Phase */
    public Detector trainingStage(Detector detec, boolean printTrain) throws Exception {
        System.out.println("\t1- Training Stage");
        System.out.println("\t\tTraining with " + detec.trainInstances.numInstances() + " instances.");
        detec.trainClassifiers(printTrain);

        System.out.println("\n\tEnd of training stage\n\n");
//        System.out.println("------------------------------------------------------------------------");

        return detec;
    }

    /* Evaluation Phase */
    public Detector evaluationStage(String stage, Detector detec, boolean printEvaluation, boolean showProgress) throws Exception {
        TimeLogger.start(stage);

        System.out.println("\t2- Evaluation Stage");
        detec.evaluateClassifiersPerCluster(stage, printEvaluation, showProgress);

        System.out.println("\tEnd of evaluation stage\n\n");
//        System.out.println("------------------------------------------------------------------------");

        TimeLogger.stop(stage);

        return detec;
    }

    /* Test Phase */
    public Detector testStage(String stage, Detector detec, boolean advices, boolean printEvaluation, boolean showProgress, int[] features) throws Exception {
        TimeLogger.start(stage);

        System.out.println("\t3- Testing Stage");
        System.out.println("\t\tTesting with " + detec.getCountTestInstances() + " instances.\n");

        detec.resetTestConters();
        detec.clusterAndTestSample(stage, advices, true, true, printEvaluation, showProgress, features, AdviceEnum.REQUEST_ADVICE);

        System.out.println("\tEnd of testing stage");

        if(stage.equals("Testing Stage")) {
            System.out.println("\tTotal conflicts found: " + getConflitos());
            detec.initialConflictsNumber = getConflitos();
            detec.lastConflictsNumber = getConflitos(); // o last mede o estado anterior, se essa é a primeira exec então logo vai ser o ultimo estado

            System.out.println("\tTotal sample without conflicts: " + (detec.getCountTestInstances()-getConflitos()));
//            System.out.println("\tAdded instances: " + detec.getInstanciasAdicionadas());
//            System.out.println("\tNew TrainInstances size: " + detec.trainInstances.size());

            // Obtivemos as medias de cada cluster, agora temos que calcular a media geral dos clusters
//            double totalAverageAccuracy = detec.getSumAverageAccuracyInitialTest()/detec.getCountTestAverages();
//            detec.setTotalAverageAccuracyInitialTest(totalAverageAccuracy);
//            System.out.println("\n\tTotal average Accuracy in the initial Testing Stage: " + totalAverageAccuracy);
//
//            double totalAverageF1Score = detec.getSumAverageF1ScoreInitialTest()/detec.getCountTestAverages();
//            detec.setTotalAverageF1ScoreInitialTest(totalAverageF1Score);
//            System.out.println("\tTotal average F1-Score in the initial Testing Stage: " + totalAverageF1Score);
//
//            Zerando o contador das medias de test para proxima etapa
//            detec.setCountTestAverages(0);

            double initialTestF1Score = Advice.calculateTestF1Score();
            System.out.println("\n\tF1-Score in the initial Testing Stage: " + initialTestF1Score);
            detec.setInitialTestF1Score(initialTestF1Score);
            detec.setLastTestF1Score(initialTestF1Score);   // o last mede o estado anterior, se essa é a primeira exec então logo vai ser o ultimo estado

            double initialTestAccuracy = Advice.calculateTestAccuracy();
            System.out.println("\n\tAccuracy in the initial Testing Stage: " + initialTestAccuracy);
            detec.setInitialTestAccuracy(initialTestAccuracy);
            detec.setLastTestAccuracy(initialTestAccuracy); // o last mede o estado anterior, se essa é a primeira exec então logo vai ser o ultimo estado

            dataSaver.buildPerformanceCSV("testResultsReport.csv", indexConselho, null, initialTestF1Score, initialTestAccuracy, getConflitos());
        } else if(stage.equals("Testing Stage - Final")) {
            double finalTestF1Score = Advice.calculateTestF1Score();
            System.out.println("\n\tF1-Score in the initial Testing Stage: " + finalTestF1Score);
            detec.setFinalTestF1Score(finalTestF1Score);

            double finalTestAccuracy = Advice.calculateTestAccuracy();
            System.out.println("\n\tAccuracy in the initial Testing Stage: " + finalTestAccuracy);
            detec.setFinalTestAccuracy(finalTestAccuracy);

            dataSaver.buildPerformanceCSV("testResultsReport.csv", indexConselho, null, finalTestF1Score, finalTestAccuracy, getConflitos());

//            double totalAverageAccuracy = detec.getSumAverageAccuracyFinalTest()/detec.getCountTestAverages();
//            detec.setTotalAverageAccuracyFinalTest(totalAverageAccuracy);
//            System.out.println("\tTotal average Accuracy in the final Testing Stage: " + totalAverageAccuracy);
//            double totalAverageF1Score = detec.getSumAverageF1ScoreFinalTest()/detec.getCountTestAverages();
//            detec.setTotalAverageF1ScoreFinalTest(totalAverageF1Score);
//            System.out.println("\tTotal average F1-Score in the final Testing Stage: " + totalAverageF1Score);
//            detec.setCountTestAverages(0);
        }

        return detec;
    }

    public Detector retestStage(String stage, Detector detec, ConselorsDTO conselorsDTO, boolean printEvaluation, boolean showProgress) throws Exception {
        indexConselho++;

        TimeLogger.start("Retest Stage");

        detec.resetTestConters();
        Advice.resetConters();
        resetConflicts();

        detec.clusterAndRetest();

        System.out.println("\tEnd of testing stage");

        detec.currentConflictsNumber = getConflitos();

        double currentTestF1Score = Advice.calculateTestF1Score();
        System.out.println("\n\tF1-Score in the current Testing Stage: " + currentTestF1Score);
        detec.setCurrentTestF1Score(currentTestF1Score);

        double currentTestAccuracy = Advice.calculateTestAccuracy();
        System.out.println("\n\tAccuracy in the current Testing Stage: " + currentTestAccuracy);
        detec.setCurrentTestAccuracy(currentTestAccuracy);

        dataSaver.buildPerformanceCSV("testResultsReport.csv", indexConselho, conselorsDTO.getId_sample(), currentTestF1Score, currentTestAccuracy, getConflitos());

        TimeLogger.stop("Retest Stage");
        System.out.println("------------------------------------------------------------------------");

        return detec;
    }

    public Detector trainEvaluateAndTest(String stage, Detector D2, boolean printEvaluation, boolean printTrain, boolean advices, boolean showProgress, int[] features) throws Exception {
        /* Train Phase*/
        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Train");
        System.out.println("------------------------------------------------------------------------");
        System.out.println("Treinamento com " + D2.trainInstances.numInstances() + " instâncias.");
        D2.trainClassifiers(printTrain);
        System.out.println("FIM TrainClassifiers");

        /* Evaluation Phase */
        D2.evaluateClassifiersPerCluster("beforeAdvice", printEvaluation, showProgress);
        System.out.println("FIM EvaluateClassifiersPerCluster");

        /* Test Phase */
        D2.clusterAndTestSample(stage, advices, true, true, printEvaluation, showProgress, features, AdviceEnum.REQUEST_ADVICE);
        System.out.println("FIM ClusterAndTestSample");
//        int VP = 0;
//        int VN = 0;
//        int FP = 0;
//        int FN = 0;
        for (DetectorClusterService d : D2.getClusters()) {
            System.out.println("---- Cluster " + d.getClusterNum() + ":");
            for (DetectorClassifier c : d.getClassifiers()) {
                if (c.isSelected()) {
                    System.out.println("[X]" + c.getName()
                            + " - " + c.getEvaluationF1Score() // antes era + " - " + c.getTestAccuracy()
                            + " (VP;VN;FP;FN) = "
                            + "("
                            + c.getVP()
                            + ";" + c.getVN()
                            + ";" + c.getFP()
                            + ";" + c.getFN()
                            + ") = ("
                            + (c.getVP() + c.getVN() + c.getFP() + c.getFN())
                            + "/" + D2.getCountTestInstances() + ")");
                    /* Atualiza Totais*/
                }
            }
        }

        System.out.println("------------------------------------------------------------------------");
        System.out.println("  --  Test Summary: [Solucionados "+ D2.getGoodAdvices()+"/"+ getConflitos() + " conflitos de " + (D2.getVP() + D2.getVN() + D2.getFP() + D2.getFN()) + " classificações.] \n "
                + "VP	VN	FP	FN	F1Score \n"
                + D2.getVP() + ";" + D2.getVN() + ";" + D2.getFP() + ";" + D2.getFN() + ";" + String.valueOf(D2.getDetectionF1Score()).replace(".", ","));
        System.out.println("------------------------------------------------------------------------");

        System.out.println("FIM treino");
        return D2;
    }

}
