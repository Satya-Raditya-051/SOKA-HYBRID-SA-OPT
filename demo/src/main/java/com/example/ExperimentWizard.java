package com.example;

import java.util.Objects;
import java.util.Scanner;

public final class ExperimentWizard {

    public ExperimentConfig prompt(Scanner scanner) {
        Objects.requireNonNull(scanner, "scanner");

        System.out.println("=== CloudSim Plus: wizard eksperimen ===");
        Scenario[] scenarios = Scenario.values();
        for (int i = 0; i < scenarios.length; i++) {
            System.out.printf("%d) %s%n", i + 1, scenarios[i].label());
        }
        int scenarioChoice = readChoice(scanner, "Pilih skenario (1-" + scenarios.length + "): ",
                1, scenarios.length);

        AlgorithmChoice[] algorithms = AlgorithmChoice.values();
        for (int i = 0; i < algorithms.length; i++) {
            System.out.printf("%d) %s%n", i + 1, algorithms[i].label());
        }
        int algorithmChoice = readChoice(scanner, "Pilih algoritma (1-" + algorithms.length + "): ",
                1, algorithms.length);
        int seed = readChoice(scanner, "Pilih seed (1-10): ", 1, 10);

        return new ExperimentConfig(
                scenarios[scenarioChoice - 1],
                algorithms[algorithmChoice - 1],
                seed);
    }

    private static int readChoice(Scanner scanner, String prompt, int minimum, int maximum) {
        while (true) {
            System.out.print(prompt);
            if (!scanner.hasNextLine()) {
                throw new IllegalStateException("Input ended before the experiment choices were complete.");
            }

            String input = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(input);
                if (value >= minimum && value <= maximum) {
                    return value;
                }
            } catch (NumberFormatException ignored) {
            }
            System.out.printf("Pilihan tidak valid. Masukkan angka %d-%d.%n", minimum, maximum);
        }
    }
}
