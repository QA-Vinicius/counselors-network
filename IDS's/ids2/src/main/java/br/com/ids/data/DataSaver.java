package br.com.ids.data;

import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;

@Component
public class DataSaver {

    // Metodo para criar o arquivo CSV no qual os dados de monitoramento do f1score na etapa de avaliacao vao ser armazenados
    public void createEvaluationPerformanceCSV(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.append("Id_Conflict,F1-Score\n");
        fileWriter.flush();
        fileWriter.close();
    }

    // Metodo para criar o arquivo CSV no qual os dados de monitoramento do f1score na etapa de teste vao ser armazenados
    public void createTestPerformanceCSV(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.append("Stage,F1-Score,Accuracy,Number_Conflicts\n");
        fileWriter.flush();
        fileWriter.close();
    }

    public void createCalculatedTestMetrics(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.append("Id_Sample,ClassifiersResult,TrueResult,Metric\n");
        fileWriter.flush();
        fileWriter.close();
    }

    public void createCalculatedRetestMetrics(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.append("Id_Sample,ClassifiersResult,TrueResult,Metric\n");
        fileWriter.flush();
        fileWriter.close();
    }

    public void createCalculatedAdviceMetrics(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.append("Id_Sample,AdviceResult,TrueResult,Metric,SendedBy\n");
        fileWriter.flush();
        fileWriter.close();
    }

    public void buildTestMetricsCSV(String file, int id_sample, double classifiersResult, double trueResult, String metric) throws IOException {
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(id_sample + "," + classifiersResult + "," + trueResult + "," + metric + "\n");
        fileWriter.flush();
        fileWriter.close();
    }

    public void buildRetestMetricsCSV(String file, int id_sample, double classifiersResult, double trueResult, String metric) throws IOException {
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(id_sample + "," + classifiersResult + "," + trueResult + "," + metric + "\n");
        fileWriter.flush();
        fileWriter.close();
    }

    public void buildAdvicesMetricsCSV(String file, int id_sample, double adviceResult, double trueResult, String metric, String ids) throws IOException {
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(id_sample + "," + adviceResult + "," + trueResult + "," + metric + "," + ("IDS " + ids) + "\n");
        fileWriter.flush();
        fileWriter.close();
    }

    // Metodo responsavel por salvar o f1score de avaliacao ao longo do fluxo, apos aprender com cada conflito
    public void buildPerformanceCSV(String file, int id_conflict, double currentF1Score) throws IOException {
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(id_conflict + "," + currentF1Score + "\n");
        fileWriter.flush();
        fileWriter.close();
    }

    // Metodo responsavel por salvar o f1score e o id da amostra de teste ao longo do fluxo, apos aprender com cada conflito
    public void buildPerformanceCSV(String file, int id_advice, Integer id_sample, double currentF1Score, double currentAccuracy, int numConflitos) throws IOException {
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(id_advice + "," + id_sample + "," + currentF1Score + "," + currentAccuracy + "," + numConflitos + "\n");
        fileWriter.flush();
        fileWriter.close();
    }
}
