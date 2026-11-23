package com.example;

public enum AlgorithmChoice {
    BASELINE("Baseline (Round Robin broker)"),
    SIMULATED_ANNEALING("Simulated Annealing"),
    SA_OPT2("Simulated Annealing + OPT-2");

    private final String label;

    AlgorithmChoice(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
