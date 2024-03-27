package br.com.ids;

import br.com.ids.domain.Detector;
import br.com.ids.domain.DetectorClassifier;
import br.com.ids.dto.ConselorsDTO;
import br.com.ids.enuns.AdviceEnum;
import br.com.ids.producer.KafkaAdviceProducer;
import br.com.ids.service.DetectorClusterService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.kafka.core.KafkaTemplate;
import weka.core.Instances;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Arrays;

@SpringBootApplication
public class IntusionDetectionApplication {
	static final String NORMAL_CLASS = "BENIGN";
	public static void main(String[] args) throws Exception {

		ConfigurableApplicationContext context = SpringApplication.run(IntusionDetectionApplication.class, args);
		IntusionDetectionApplication wekaMain = new IntusionDetectionApplication();
		KafkaTemplate<String, ConselorsDTO> kafkaTemplate = context.getBean(KafkaTemplate.class);
		KafkaAdviceProducer kafkaAdviceProducer = context.getBean(KafkaAdviceProducer.class);
		int[] oneR_Detector1 = new int[]{34, 48, 19, 12, 53}; //79, 40, 68, 13, 55

		/*
		 * Nesta etapa instanciamos o primeiro Detector e seus respectivos dataSets de treino, avaliação e testes
		 * essa etapa deve ser iniciada ao instanciar um IDS
		 * */
		Instances trainInstances = wekaMain.leadAndFilter(false, "4output1.csv", oneR_Detector1);
		Instances evaluationInstances = wekaMain.leadAndFilter(false, "5output1k.csv", oneR_Detector1);
		Instances testInstances = wekaMain.leadAndFilter(false, "6output1k.csv", oneR_Detector1);

		Detector D2 = new Detector(kafkaAdviceProducer, 2, trainInstances, evaluationInstances, testInstances, NORMAL_CLASS);


		// Instancia a quantidade  clusters
		D2.createClusters(5, 2);

		System.out.println("\n######## Detector 1");

		// Zera todas as variaveis para avaliação
		D2.resetConters();

		//Treina seus classificadores com o dataset de treino
		D2 = trainEvaluateAndTest(D2, false, false, true, true, oneR_Detector1);
	}

	private static Detector trainEvaluateAndTest(Detector D2, boolean printEvaluation, boolean printTrain, boolean advices, boolean showProgress, int[] features) throws Exception {
		/* Train Phase*/
		System.out.println("------------------------------------------------------------------------");
		System.out.println("  --  Train");
		System.out.println("------------------------------------------------------------------------");
		System.out.println("Treinamento com " + D2.trainInstances.numInstances() + " instâncias.");
		D2.trainClassifiers(printTrain);

		/* Evaluation Phase */
		D2.evaluateClassifiersPerCluster(printEvaluation, showProgress);

		/* Test Phase */
		D2.clusterAndTestSample(advices, true, true, printEvaluation, showProgress, features, AdviceEnum.REQUEST_ADVICE);

//        int VP = 0;
//        int VN = 0;
//        int FP = 0;
//        int FN = 0;
		for (DetectorClusterService d : D2.getClusters()) {
			System.out.println("---- Cluster " + d.getClusterNum() + ":");
			for (DetectorClassifier c : d.getClassifiers()) {
				if (c.isSelected()) {
					System.out.println("[X]" + c.getName()
							+ " - " + c.getEvaluationF1Score() // antes era + " - " + c.getTestAccuracy()
							+ " (VP;VN;FP;FN) = "
							+ "("
							+ c.getVP()
							+ ";" + c.getVN()
							+ ";" + c.getFP()
							+ ";" + c.getFN()
							+ ") = ("
							+ (c.getVP() + c.getVN() + c.getFP() + c.getFN())
							+ "/" + D2.getCountTestInstances() + ")");
					/* Atualiza Totais*/
				}

			}

		}
		System.out.println("------------------------------------------------------------------------");
		System.out.println("  --  Test Summary: [Solucionados "+ D2.getGoodAdvices()+"/"+ D2.getConflitos() + " conflitos de " + (D2.getVP() + D2.getVN() + D2.getFP() + D2.getFN()) + " classificações.] \n "
				+ "VP	VN	FP	FN	F1Score \n"
				+ D2.getVP() + ";" + D2.getVN() + ";" + D2.getFP() + ";" + D2.getFN() + ";" + String.valueOf(D2.getDetectionF1Score()).replace(".", ","));
		System.out.println("------------------------------------------------------------------------");

		return D2;
	}

	public Instances leadAndFilter(boolean printSelection, String file, int[] featureSelection) throws Exception {
		Instances instances = new Instances(readDataFile(file));
		if (featureSelection.length > 0) {
			instances = applyFilterKeep(instances, featureSelection);
			if (printSelection) {
				System.out.println(Arrays.toString(featureSelection) + " - ");
			}
		}
		return instances;
	}
	public static BufferedReader readDataFile(String filename) {
		BufferedReader inputReader = null;

		try {
			inputReader = new BufferedReader(new FileReader(filename));
		} catch (FileNotFoundException ex) {
			System.err.println("File not found: " + filename);
		}

		return inputReader;
	}

	public static Instances applyFilterKeep(Instances instances, int[] fs) {
		Arrays.sort(fs);
		for (int i = instances.numAttributes() - 1; i > 0; i--) {
			if (instances.numAttributes() <= fs.length) {
				System.err.println("O número de features (" + instances.numAttributes() + ") precisa ser maior que o filtro (" + fs.length + ").");
				return instances;
			}
			boolean deletar = true;
			for (int j : fs) {
				if (i == j) {
					deletar = false;
//                    System.out.println("Manter [" + i + "]:" + instances.attribute(i));
				}
			}
			if (deletar) {
				instances.deleteAttributeAt(i - 1);
			}
		}
		return instances;
	}
}