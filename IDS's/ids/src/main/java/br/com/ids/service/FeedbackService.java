package br.com.ids.service;

import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
import org.springframework.stereotype.Service;

@Service
public class FeedbackService {

    Detector detector;

    public void learnWithFeedback(ConselorsDTO value) throws Exception {
//        detector.trainInstances.add(value.getSample());
        /* Treina Todos Classificadores Novamente */
        detector.trainClassifiers(false);
        detector.evaluateClassifiersPerCluster(true, false);
        detector.selectClassifierPerCluster(false);
    }
}
