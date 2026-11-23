package com.example;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HybridSimulatedAnnealingOpt2Test {

    @Test
    public void opt2ImprovesAssignmentUntilNoPairSwapHelps() {
        SimulatedAnnealing sa = new SimulatedAnnealing(
                new double[] {10, 8, 2},
                new double[] {1, 2},
                1,
                100,
                0.001,
                0.95,
                100);
        HybridSimulatedAnnealingOpt2 hybrid = new HybridSimulatedAnnealingOpt2(sa);
        int[] initial = {0, 1, 1};
        double initialFitness = sa.fitness(initial);

        int[] optimized = hybrid.optimizeWithOpt2(initial);

        assertTrue(sa.fitness(optimized) < initialFitness);
        assertArrayEquals(new int[] {0, 1, 1}, initial);
        assertArrayEquals(optimized, hybrid.optimizeWithOpt2(optimized));
    }
}
