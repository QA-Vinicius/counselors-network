package br.com.ids.util;

import javax.print.DocFlavor;
import javax.print.DocFlavor.INPUT_STREAM;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.*;

public class GenerateReader {

    public static Reader generateInstances(double[] sample) throws Exception {
        try {
            // Convertendo o array de doubles para uma sequência de bytes
            StringBuilder stringBuilder = new StringBuilder();
            for (double d : sample) {
                stringBuilder.append(d).append(" ");
            }
            String doubleString = stringBuilder.toString();
            byte[] bytes = doubleString.getBytes(UTF_8);

            // Criando um InputStream com os bytes
            InputStream inputStream = new ByteArrayInputStream(bytes);

            // Criando um InputStreamReader para ler os bytes como caracteres
            Reader reader = new InputStreamReader(inputStream, UTF_8);
//
//            // Usando o Reader como desejado
//            int data;
//            while ((data = reader.read()) != -1) {
//                System.out.print((char) data);
//            }
//
//            // Fechando o Reader e o InputStream
            reader.close();
            inputStream.close();
            return  reader;
        } catch (Exception e) {
            throw new Exception("ERRO ENCONTRADO"+ e);
        }
    }
}