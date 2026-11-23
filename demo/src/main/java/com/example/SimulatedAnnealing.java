package com.example;

import java.util.Arrays;
import java.util.Random;

public class SimulatedAnnealing {

    private final double[] cloudletLength; // panjang tiap cloudlet (MI)
    private final double[] vmMips;         // MIPS per core untuk tiap VM
    private final int[] vmPes;             // jumlah core untuk tiap VM
    private final double initialTemp;
    private final double minTemp;
    private final double coolingRate;
    private final int iterPerTemp;
    private final Random random;

    public SimulatedAnnealing(double[] cloudletLength, double[] vmMips, int vmPes,
                              double initialTemp, double minTemp,
                              double coolingRate, int iterPerTemp) {
        this(cloudletLength, vmMips, repeatedCores(vmMips.length, vmPes),
                initialTemp, minTemp, coolingRate, iterPerTemp, 42);
    }

    public SimulatedAnnealing(double[] cloudletLength, double[] vmMips, int vmPes,
                              double initialTemp, double minTemp,
                              double coolingRate, int iterPerTemp, long seed) {
        this(cloudletLength, vmMips, repeatedCores(vmMips.length, vmPes),
                initialTemp, minTemp, coolingRate, iterPerTemp, seed);
    }

    public SimulatedAnnealing(double[] cloudletLength, double[] vmMips, int[] vmPes,
                              double initialTemp, double minTemp,
                              double coolingRate, int iterPerTemp, long seed) {
        if (cloudletLength.length == 0) {
            throw new IllegalArgumentException("At least one cloudlet is required.");
        }
        if (vmMips.length == 0 || vmMips.length != vmPes.length) {
            throw new IllegalArgumentException("VM MIPS and core arrays must have the same non-zero length.");
        }
        if (Arrays.stream(vmMips).anyMatch(mips -> mips <= 0)
                || Arrays.stream(vmPes).anyMatch(cores -> cores <= 0)) {
            throw new IllegalArgumentException("VM MIPS and core counts must be positive.");
        }

        this.cloudletLength = cloudletLength.clone();
        this.vmMips = vmMips.clone();
        this.vmPes = vmPes.clone();
        this.initialTemp = initialTemp;
        this.minTemp = minTemp;
        this.coolingRate = coolingRate;
        this.iterPerTemp = iterPerTemp;
        this.random = new Random(seed);
    }

    public int[] solve() {
        int[] current = randomSolution();
        double currentCost = fitness(current);

        int[] best = current.clone();
        double bestCost = currentCost;

        double temp = initialTemp;
        while (temp > minTemp) {
            for (int i = 0; i < iterPerTemp; i++) {
                int[] candidate = neighbor(current);
                double candidateCost = fitness(candidate);
                double delta = candidateCost - currentCost;

                // Terima jika lebih baik, atau lebih buruk dengan peluang e^(-delta/temp)
                if (delta < 0 || Math.exp(-delta / temp) > random.nextDouble()) {
                    current = candidate;
                    currentCost = candidateCost;

                    if (currentCost < bestCost) {
                        best = current.clone();
                        bestCost = currentCost;
                    }
                }
            }
            temp *= coolingRate;
        }
        System.out.printf("Makespan estimasi SA: %.2f detik%n", bestCost);
        return best;
    }

    private int[] randomSolution() {
        int[] sol = new int[cloudletLength.length];
        for (int i = 0; i < sol.length; i++) {
            sol[i] = random.nextInt(vmMips.length);
        }
        return sol;
    }

    // Estimasi makespan: dibatasi oleh beban total per VM
    // dan oleh cloudlet terpanjang (yang hanya bisa memakai 1 core)
    public double fitness(int[] sol) {
        double[] load = new double[vmMips.length];
        double[] maxLen = new double[vmMips.length];
        for (int i = 0; i < sol.length; i++) {
            int v = sol[i];
            load[v] += cloudletLength[i];
            maxLen[v] = Math.max(maxLen[v], cloudletLength[i]);
        }
        double makespan = 0;
        for (int v = 0; v < load.length; v++) {
            double byLoad = load[v] / (vmMips[v] * vmPes[v]);
            double byLongest = maxLen[v] / vmMips[v];
            makespan = Math.max(makespan, Math.max(byLoad, byLongest));
        }
        return makespan;
    }

    double[] cloudletLengths() {
        return cloudletLength.clone();
    }

    double[] vmMips() {
        return vmMips.clone();
    }

    int[] vmPes() {
        return vmPes.clone();
    }

    private int[] neighbor(int[] sol) {
        int[] copy = sol.clone();
        int cloudletIdx = random.nextInt(copy.length);
        copy[cloudletIdx] = random.nextInt(vmMips.length);
        return copy;
    }

    private static int[] repeatedCores(int vmCount, int cores) {
        int[] vmPes = new int[vmCount];
        Arrays.fill(vmPes, cores);
        return vmPes;
    }
}