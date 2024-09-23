package br.com.ids.enuns;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum IdsEnum {
    DETECTOR_1(1), DETECTOR_2(2), DETECTOR_3(3) ;
    private final int enumIdentifier;
}
