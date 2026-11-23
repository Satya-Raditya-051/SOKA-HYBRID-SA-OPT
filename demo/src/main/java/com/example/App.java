package com.example;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.cloudsimplus.brokers.DatacenterBroker;
import org.cloudsimplus.brokers.DatacenterBrokerSimple;
import org.cloudsimplus.builders.tables.CloudletsTableBuilder;
import org.cloudsimplus.cloudlets.Cloudlet;
import org.cloudsimplus.cloudlets.CloudletSimple;
import org.cloudsimplus.core.CloudSimPlus;
import org.cloudsimplus.datacenters.DatacenterSimple;
import org.cloudsimplus.hosts.Host;
import org.cloudsimplus.hosts.HostSimple;
import org.cloudsimplus.resources.Pe;
import org.cloudsimplus.resources.PeSimple;
import org.cloudsimplus.schedulers.cloudlet.CloudletSchedulerSpaceShared;
import org.cloudsimplus.schedulers.vm.VmSchedulerSpaceShared;
import org.cloudsimplus.vms.Vm;
import org.cloudsimplus.vms.VmSimple;

public class App {

    private static final double[] VM_TYPE_MIPS = {500, 800, 1000, 1300, 1600};
    private static final int[] VM_TYPE_PES = {1, 1, 2, 2, 3};
    private static final int HOST_COUNT = 20;
    private static final int HOST_PES = 6;

    public static void main(String[] args) throws IOException {
        ExperimentConfig config = new ExperimentWizard().prompt(new Scanner(System.in));
        long[] taskLengths = new WorkloadLoader().load(config.scenario());
        runExperiment(config, taskLengths);
    }

    private static void runExperiment(ExperimentConfig config, long[] taskLengths) {
        CloudSimPlus simulation = new CloudSimPlus();
        List<Host> hosts = createHosts();
        new DatacenterSimple(simulation, hosts);

        DatacenterBroker broker = new DatacenterBrokerSimple(simulation);
        List<Vm> vmList = createVms(config.scenario().vmCount());
        List<Cloudlet> cloudletList = createCloudlets(taskLengths);

        if (config.algorithm() != AlgorithmChoice.BASELINE) {
            double[] lengths = new double[taskLengths.length];
            for (int i = 0; i < taskLengths.length; i++) {
                lengths[i] = taskLengths[i];
            }
            double[] vmMips = new double[vmList.size()];
            int[] vmPes = new int[vmList.size()];
            for (int i = 0; i < vmList.size(); i++) {
                int type = i % VM_TYPE_MIPS.length;
                vmMips[i] = VM_TYPE_MIPS[type];
                vmPes[i] = VM_TYPE_PES[type];
            }

            SimulatedAnnealing sa = new SimulatedAnnealing(
                    lengths, vmMips, vmPes, 100, 0.001, 0.95, 100, config.seed());
            int[] mapping = config.algorithm() == AlgorithmChoice.SA_OPT2
                    ? new HybridSimulatedAnnealingOpt2(sa).solve()
                    : sa.solve();
            for (int i = 0; i < cloudletList.size(); i++) {
                cloudletList.get(i).setVm(vmList.get(mapping[i]));
            }
        }

        broker.submitVmList(vmList);
        broker.submitCloudletList(cloudletList);
        simulation.start();
        new CloudletsTableBuilder(broker.getCloudletFinishedList()).build();

        double makespan = broker.getCloudletFinishedList().stream()
                .mapToDouble(Cloudlet::getFinishTime)
                .max()
                .orElse(0);
        System.out.printf("%nSkenario: %s%nAlgoritma: %s | Seed: %d%n",
                config.scenario().label(), config.algorithm().label(), config.seed());
        System.out.printf("Cloudlet selesai: %d/%d | Makespan simulasi: %.2f detik%n",
                broker.getCloudletFinishedList().size(), taskLengths.length, makespan);
    }

    private static List<Host> createHosts() {
        List<Host> hosts = new ArrayList<>(HOST_COUNT);
        for (int hostIndex = 0; hostIndex < HOST_COUNT; hostIndex++) {
            double hostMips = hostIndex < HOST_COUNT / 2 ? 2000 : 2500;
            List<Pe> peList = new ArrayList<>(HOST_PES);
            for (int pe = 0; pe < HOST_PES; pe++) {
                peList.add(new PeSimple(hostMips));
            }
            hosts.add(new HostSimple(16_384, 100_000, 1_000_000, peList)
                    .setVmScheduler(new VmSchedulerSpaceShared()));
        }
        return hosts;
    }

    private static List<Vm> createVms(int vmCount) {
        List<Vm> vmList = new ArrayList<>(vmCount);
        for (int i = 0; i < vmCount; i++) {
            int type = i % VM_TYPE_MIPS.length;
            vmList.add(new VmSimple(VM_TYPE_MIPS[type], VM_TYPE_PES[type])
                    .setRam(512)
                    .setBw(1024)
                    .setSize(10_240)
                    .setCloudletScheduler(new CloudletSchedulerSpaceShared()));
        }
        return vmList;
    }

    private static List<Cloudlet> createCloudlets(long[] taskLengths) {
        List<Cloudlet> cloudletList = new ArrayList<>(taskLengths.length);
        for (long taskLength : taskLengths) {
            cloudletList.add(new CloudletSimple(taskLength, 1));
        }
        return cloudletList;
    }
}
