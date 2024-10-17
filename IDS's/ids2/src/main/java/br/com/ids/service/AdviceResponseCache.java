package br.com.ids.service;

import br.com.ids.dto.ConselorsDTO;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AdviceResponseCache {

    private final Map<Integer, List<ConselorsDTO>> responseCache = new HashMap<>();

    // Metodo para guardar cache dos conselhos recebidos para cada amostra
    public void storeAdvice(ConselorsDTO dto) {
        responseCache.computeIfAbsent(dto.getId_sample(), k -> new ArrayList<>()).add(dto);
    }

    // Criterio de parada para armazenar conselhos (limitando ao numero maximo de conselhos que pode receber para cada amostra)
    public boolean stoppingCriterion(int sampleId) {
        return responseCache.get(sampleId).size() >= 2; // Exemplo: 2 conselhos recebidos
    }

    // Compara os conselhos recebidos e escolhe o que possui o maior F1-Score entre eles
    public ConselorsDTO getBestAdvice(int sampleId) {
        return responseCache.get(sampleId).stream()
                .max(Comparator.comparing(ConselorsDTO::getF1score))
                .orElseThrow(() -> new RuntimeException("No advices found for sample: " + sampleId));
    }
}
