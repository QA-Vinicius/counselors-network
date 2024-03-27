package br.com.ids.enuns;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum AdviceEnum {
    REQUEST_ADVICE("REQUEST_ADVICE"), ADVICE("ADVICE"), FEEDBACK("FEEDBACK");
    private final String enumIdentifier;
}
