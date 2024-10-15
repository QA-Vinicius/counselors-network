package br.com.ids.service;

import br.com.ids.dto.ConselorsDTO;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class AdviceResponseCache {

    private final Map<Integer, List<ConselorsDTO>> responseCache = new HashMap<>();

    // Armazena os conselhos em um cache temporário, agrupados por ID da amostra
    public void storeAdvice(ConselorsDTO dto) {
        responseCache.computeIfAbsent(dto.getId_sample(), k -> new ArrayList<>()).add(dto);
    }

    // Verifica se já recebeu todas as respostas ou se pode prosseguir
    public boolean isReadyToLearn(int sampleId) {
        // Aqui você pode colocar uma lógica para definir quando pode aprender
        // Exemplo: pode esperar por um certo número de respostas ou por um tempo limite
        return responseCache.get(sampleId).size() >= 2; // Exemplo: 2 conselhos recebidos
    }

    // Seleciona o conselho com o score mais alto (confiável)
    public ConselorsDTO getBestAdvice(int sampleId) {
        return responseCache.get(sampleId).stream()
                .max(Comparator.comparing(ConselorsDTO::getF1score))
                .orElseThrow(() -> new RuntimeException("No advices found for sample: " + sampleId));
    }
}
