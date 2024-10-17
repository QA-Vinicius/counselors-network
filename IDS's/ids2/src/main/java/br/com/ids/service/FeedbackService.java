package br.com.ids.service;

import br.com.ids.domain.Detector;
import br.com.ids.dto.ConselorsDTO;
import org.springframework.stereotype.Service;
import weka.core.Copyable;
import weka.core.Instances;

@Service
public class FeedbackService {

    Detector detector;

    public void learnWithFeedback(ConselorsDTO value) throws Exception {
//        detector.trainInstances.add(GenerateAdviceService.generateInstance(value));
        /* Treina Todos Classificadores Novamente */
        detector.trainClassifiers(false);
        detector.evaluateClassifiersPerCluster("beforeAdvice", true, false);
        detector.selectClassifierPerCluster(false);
    }
}
