package br.com.ids.util;

public class ConflictManager {
    private static int conflitos = 0;

    public static int getConflitos() {
        return conflitos;
    }

    public static void increasesConflicts() {
        conflitos++;
    }

    public static void resetConflicts() {
        conflitos = 0;
    }

    public static int consumerStoppingCriterion() {
        return conflitos*2;
    }
}
