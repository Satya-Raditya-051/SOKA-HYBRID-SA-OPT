package com.example;

import java.util.Arrays;
import java.util.Objects;

public final class HybridSimulatedAnnealingOpt2 {

    private final SimulatedAnnealing simulatedAnnealing;

    public HybridSimulatedAnnealingOpt2(SimulatedAnnealing simulatedAnnealing) {
        this.simulatedAnnealing = Objects.requireNonNull(simulatedAnnealing);
    }

    public int[] solve() {
        int[] saSolution = simulatedAnnealing.solve();
        int[] optimizedSolution = optimizeWithOpt2(saSolution);
        System.out.printf("Makespan estimasi SA + OPT-2: %.2f detik%n",
                simulatedAnnealing.fitness(optimizedSolution));
        return optimizedSolution;
    }

    int[] optimizeWithOpt2(int[] initialSolution) {
        int[] current = initialSolution.clone();
        double[] cloudletLengths = simulatedAnnealing.cloudletLengths();
        double[] vmMips = simulatedAnnealing.vmMips();
        int[] vmPes = simulatedAnnealing.vmPes();
        SearchState state = createState(current, cloudletLengths, vmMips, vmPes);

        while (true) {
            int bestFirst = -1;
            int bestSecond = -1;
            double bestFitness = state.fitness;
            int[] topVmIndices = topThreeVmIndices(state.vmMakespan);

            for (int first = 0; first < current.length - 1; first++) {
                for (int second = first + 1; second < current.length; second++) {
                    int firstVm = current[first];
                    int secondVm = current[second];
                    if (firstVm == secondVm) {
                        continue;
                    }

                    double firstMax = state.maxLength[firstVm];
                    if (state.maxIndex[firstVm] == first) {
                        firstMax = state.secondMaxLength[firstVm];
                    }
                    firstMax = Math.max(firstMax, cloudletLengths[second]);

                    double secondMax = state.maxLength[secondVm];
                    if (state.maxIndex[secondVm] == second) {
                        secondMax = state.secondMaxLength[secondVm];
                    }
                    secondMax = Math.max(secondMax, cloudletLengths[first]);

                    double firstLoad = state.load[firstVm]
                            - cloudletLengths[first] + cloudletLengths[second];
                    double secondLoad = state.load[secondVm]
                            - cloudletLengths[second] + cloudletLengths[first];

                    double candidateFitness = Math.max(
                            maxUnchangedVmMakespan(topVmIndices, state.vmMakespan, firstVm, secondVm),
                            Math.max(vmMakespan(firstLoad, firstMax, firstVm, vmMips, vmPes),
                                    vmMakespan(secondLoad, secondMax, secondVm, vmMips, vmPes)));
                    if (candidateFitness < bestFitness) {
                        bestFirst = first;
                        bestSecond = second;
                        bestFitness = candidateFitness;
                    }
                }
            }

            if (bestFirst < 0) {
                return current;
            }

            int assignment = current[bestFirst];
            current[bestFirst] = current[bestSecond];
            current[bestSecond] = assignment;
            state = createState(current, cloudletLengths, vmMips, vmPes);
        }
    }

    private static SearchState createState(int[] assignment, double[] cloudletLengths,
                                           double[] vmMips, int[] vmPes) {
        double[] load = new double[vmMips.length];
        double[] maxLength = new double[vmMips.length];
        double[] secondMaxLength = new double[vmMips.length];
        int[] maxIndex = new int[vmMips.length];
        Arrays.fill(maxIndex, -1);

        for (int task = 0; task < assignment.length; task++) {
            int vm = assignment[task];
            double length = cloudletLengths[task];
            load[vm] += length;
            if (length >= maxLength[vm]) {
                secondMaxLength[vm] = maxLength[vm];
                maxLength[vm] = length;
                maxIndex[vm] = task;
            } else if (length > secondMaxLength[vm]) {
                secondMaxLength[vm] = length;
            }
        }

        double[] vmMakespan = new double[vmMips.length];
        double fitness = 0;
        for (int vm = 0; vm < vmMips.length; vm++) {
            vmMakespan[vm] = vmMakespan(load[vm], maxLength[vm], vm, vmMips, vmPes);
            fitness = Math.max(fitness, vmMakespan[vm]);
        }

        return new SearchState(load, maxLength, secondMaxLength, maxIndex, vmMakespan, fitness);
    }

    private static double vmMakespan(double load, double maxLength, int vm,
                                     double[] vmMips, int[] vmPes) {
        return Math.max(load / (vmMips[vm] * vmPes[vm]), maxLength / vmMips[vm]);
    }

    private static int[] topThreeVmIndices(double[] vmMakespan) {
        int[] top = {-1, -1, -1};
        for (int vm = 0; vm < vmMakespan.length; vm++) {
            for (int position = 0; position < top.length; position++) {
                if (top[position] < 0 || vmMakespan[vm] > vmMakespan[top[position]]) {
                    for (int shift = top.length - 1; shift > position; shift--) {
                        top[shift] = top[shift - 1];
                    }
                    top[position] = vm;
                    break;
                }
            }
        }
        return top;
    }

    private static double maxUnchangedVmMakespan(int[] topVmIndices, double[] vmMakespan,
                                                  int firstVm, int secondVm) {
        for (int vm : topVmIndices) {
            if (vm >= 0 && vm != firstVm && vm != secondVm) {
                return vmMakespan[vm];
            }
        }
        return 0;
    }

    private static final class SearchState {
        private final double[] load;
        private final double[] maxLength;
        private final double[] secondMaxLength;
        private final int[] maxIndex;
        private final double[] vmMakespan;
        private final double fitness;

        private SearchState(double[] load, double[] maxLength, double[] secondMaxLength,
                            int[] maxIndex, double[] vmMakespan, double fitness) {
            this.load = load;
            this.maxLength = maxLength;
            this.secondMaxLength = secondMaxLength;
            this.maxIndex = maxIndex;
            this.vmMakespan = vmMakespan;
            this.fitness = fitness;
        }
    }
}
