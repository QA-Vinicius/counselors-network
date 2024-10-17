package br.com.ids.data;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class DataSaver {

    public void createPerformanceCSV(File filename) throws IOException {
        FileWriter fileWriter = new FileWriter(filename);
        fileWriter.append("Id_Conflict,F1-Score");
        fileWriter.flush();
        fileWriter.close();
    }


    //Metodo responsavel por salvar
    public void buildPerformanceCSV(String file, int id_conflict, double currentF1Score) throws IOException {
        FileWriter fileWriter = new FileWriter(file, true);
        fileWriter.append(id_conflict + "," + currentF1Score + "\n");
        fileWriter.flush();
        fileWriter.close();
    }
}
