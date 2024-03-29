package br.com.ids.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import weka.core.Instance;
import weka.core.Instances;

@Builder
@Setter
@Getter
@AllArgsConstructor
public class ConselorsDTO {

    @JsonProperty("id_conselheiro")
    private int id_conselheiro;

    @JsonProperty("flag")
    private String flag;

    @JsonProperty("features")
    private int[] features;

    @JsonProperty("sample") // amostra/linha onde ocorreu o conflito
    private int sample;

    @JsonProperty("f1score")
    private double f1score;

    @JsonProperty("attack") // adaptacao para multiclass
    private boolean attack;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("feedback")
    private String feedback; //positive or negative

    @JsonProperty("deltaF1Score")
    private double deltaF1Score;

    public ConselorsDTO() {
    }

}
