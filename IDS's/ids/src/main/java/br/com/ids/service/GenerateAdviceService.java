package br.com.ids.service;

import br.com.ids.dto.ConselorsDTO;
import org.springframework.stereotype.Service;

@Service
public class GenerateAdviceService {
    public void generatesAdvice(ConselorsDTO conselorsDTO){ //o cara QUER CONSELHO  IDS-2
        // Gerar conselho
        /*Aplicar o Kmeans  weka.clusterers.SimpleKMeans.clusterInstance
        *   A partir de features analisadas em conselho(sample)
        *       int cluster = kmeans.cluster(sample)
        *       var selectedClassfier[] = getBestClassofier(cluster) com.mycompany.counselorsnetwork.DetectorClassifier
        *       result = selectedClassfier.classify (> 1 utilizar laço de repetição)
        *       avaliar respostas dos classificadores, e seguir com a resposta majoritaria
        *       kafka.send.(resposta)
        * */
    }

    public void sendAdvice(){

    }


}
