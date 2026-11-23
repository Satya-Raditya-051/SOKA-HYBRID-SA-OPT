package com.example;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.junit.Test;

public class ExperimentWorkflowTest {

    @Test
    public void loadsSelectedBenchmarkDataAndConvertsNasaRecords() throws IOException {
        WorkloadLoader loader = new WorkloadLoader();
        long[] nasa = null;
        for (Scenario scenario : Scenario.values()) {
            long[] workload = loader.load(scenario);
            assertEquals(scenario.taskCount(), workload.length);
            for (long length : workload) {
                assertTrue(length > 0);
            }
            if (scenario == Scenario.NASA_750_JOBS_50_VMS) {
                nasa = workload;
            }
        }

        assertEquals(185_728_000L, nasa[0]);
    }

    @Test
    public void wizardRetriesInvalidSelectionsAndAcceptsSeedTen() {
        Scanner input = new Scanner("9\n1\n4\n3\n0\n10\n");

        ExperimentConfig config = new ExperimentWizard().prompt(input);

        assertEquals(Scenario.GOCJ_500_TASKS_50_VMS, config.scenario());
        assertEquals(AlgorithmChoice.SA_OPT2, config.algorithm());
        assertEquals(10, config.seed());
    }

    @Test
    public void simulatedAnnealingProducesRepeatableResultsForSameSeed() {
        double[] lengths = {1000, 2500, 4000, 5500, 7000};
        double[] mips = {500, 1000, 1500};
        int[] pes = {1, 2, 2};

        int[] first = new SimulatedAnnealing(lengths, mips, pes,
                100, 0.001, 0.95, 100, 7).solve();
        int[] second = new SimulatedAnnealing(lengths, mips, pes,
                100, 0.001, 0.95, 100, 7).solve();

        assertArrayEquals(first, second);
    }

    @Test
    public void wizardRunsSelectedHybridScenarioInCloudSim() throws IOException {
        java.io.InputStream originalInput = System.in;
        PrintStream originalOutput = System.out;
        ByteArrayOutputStream capturedOutput = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream("1\n3\n10\n".getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(capturedOutput, true, StandardCharsets.UTF_8));

            App.main(new String[0]);

            String output = capturedOutput.toString(StandardCharsets.UTF_8);
            assertTrue(output.contains("Simulated Annealing + OPT-2"));
            assertTrue(output.contains("Cloudlet selesai: 500/500"));
        } finally {
            System.setIn(originalInput);
            System.setOut(originalOutput);
        }
    }
}
