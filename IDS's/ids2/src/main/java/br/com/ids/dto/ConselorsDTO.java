package br.com.ids.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Setter
@Getter
@AllArgsConstructor
public class ConselorsDTO {

    @JsonProperty("id_conselheiro")
    private String id_conselheiro;

    @JsonProperty("flag")
    private String flag;

    @JsonProperty("features")
    private int[] features;

    @JsonProperty("sample") // amostra/linha onde ocorreu o conflito
    private double[] sample;

    @JsonProperty("f1score")
    private double f1score;

//    @JsonProperty("attack") // adaptacao para multiclass
//    private boolean attack;

    @JsonProperty("result") // adaptacao para multiclass
    private double result;

    @JsonProperty("timestamp")
    private long timestamp;

    @JsonProperty("feedback")
    private String feedback; //positive or negative

    @JsonProperty("deltaF1Score")
    private double deltaF1Score;

    public ConselorsDTO() {
    }

    public double[] getSample() {
        return sample;
    }

    public void setSample(double[] sample) {
        this.sample = sample;
    }
}
