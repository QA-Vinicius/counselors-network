package br.com.ids.enuns;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdviceEnum {
    REQUEST_ADVICE(1), ADVICE(2), RESPONSE_ADVICE(3), FEEDBACK(4);
    private final int enumIdentifier;
}
