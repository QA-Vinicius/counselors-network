package br.com.ids.data;

import org.springframework.stereotype.Component;

import java.io.FileWriter;
import java.io.IOException;

@Component
public class DataSaver {

    // Metodo para criar o arquivo CSV no qual os dados de monitoramento do f1score vao ser armazenados
    public void createPerformanceCSV(String filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.append("Id_Conflict,F1-Score\n");
        fileWriter.flush();
        fileWriter.close();
    }

    // Metodo responsavel por salvar o f1score ao longo do fluxo, apos aprender com cada conflito
    public void buildPerformanceCSV(String file, int id_conflict, double currentF1Score) throws IOException {
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(id_conflict + "," + currentF1Score + "\n");
        fileWriter.flush();
        fileWriter.close();
    }
}
