package com.mycompany.dto;

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

    public ConselorsDTO() {
    }

}
