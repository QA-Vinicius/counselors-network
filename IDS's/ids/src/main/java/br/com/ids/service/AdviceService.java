package br.com.ids.service;

import br.com.ids.dto.ConselorsDTO;
import br.com.ids.scheduling.JobScheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import static org.apache.kafka.common.requests.DeleteAclsResponse.log;

@Service
public class AdviceService {

    @Autowired
    private JobScheduler jobScheduler;

    public void generatesAdvice(ConselorsDTO conselorsDTO) throws Exception {
        jobScheduler.processSample(conselorsDTO);
    }

    public void learnWithAdvice(ConselorsDTO conselorsDTO) throws Exception {
        jobScheduler.learnWithAdvice(conselorsDTO); // sera usado para processamento do conselho quando o solicitante receber
    }

//    public void generatesAdvice(ConselorsDTO conselorsDTO) throws Exception {
//        // Obtenha o dataset de treino
//        Instances trainInstances = clusterService.getTrainInstances();
//
//        // Verifique se o dataset não é nulo
//        if (trainInstances == null) {
//            log.error("Train instances dataset is null. Cannot proceed with generating advice.");
//            throw new NullPointerException("Train instances dataset is null.");
//        }
//
//        // Converte o array de sample para um Instance do Weka
//        Instance instance = createInstanceFromSample(conselorsDTO.getSample(), trainInstances);
//
//        // Adiciona a instância ao dataset
//        trainInstances.add(instance);
//
//        // Reavalie os classificadores
//        clusterService.evaluateClassifiers(trainInstances);
//
//        // Se necessário, treine novamente
//        clusterService.trainClassifiers(trainInstances, false);
//
//        // Classifique a nova instância
//        double classValue = classifyInstance(instance);
//
//        // Envie a resposta de volta via Kafka ou faça outra ação necessária
//        // Exemplo: Enviar a resposta para outro tópico ou armazenar a resposta
//    }
//
//    private Instance createInstanceFromSample(double[] sample, Instances dataset) {
//        // Verifique se o dataset não é nulo
//        if (dataset == null) {
//            log.error("Dataset is null. Cannot create instance from sample.");
//            throw new NullPointerException("Dataset is null.");
//        }
//
//        // Cria uma nova instância com o número de atributos do dataset
//        Instance instance = new DenseInstance(dataset.numAttributes());
//
//        // Define os valores dos atributos da nova instância com base no sample
//        for (int i = 0; i < sample.length; i++) {
//            instance.setValue(dataset.attribute(i), sample[i]);
//        }
//
//        // Define o dataset da nova instância
//        instance.setDataset(dataset);
//
//        return instance;
//    }
//
//    private double classifyInstance(Instance instance) throws Exception {
//        // Utilize o classificador que melhor se adequar ao seu sistema
//        // Por exemplo, o primeiro classificador da lista de selecionados
//        DetectorClassifier classifier = clusterService.getSelectedClassifiers().get(0);
//        return classifier.classifyInstance(instance);
//    }
}