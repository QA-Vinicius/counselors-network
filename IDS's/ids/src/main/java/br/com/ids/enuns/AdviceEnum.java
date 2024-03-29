package br.com.ids.enuns;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdviceEnum {
    REQUEST_ADVICE(1), ADVICE(2), FEEDBACK(3);
    private final int enumIdentifier;
}
