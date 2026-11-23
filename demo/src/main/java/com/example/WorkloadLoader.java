package com.example;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class WorkloadLoader {

    public long[] load(Scenario scenario) throws IOException {
        try (InputStream input = WorkloadLoader.class.getResourceAsStream(scenario.resourcePath())) {
            if (input == null) {
                throw new IOException("Dataset resource not found: " + scenario.resourcePath());
            }
            return scenario.isNasaWorkload()
                    ? loadNasa(input, scenario.taskCount())
                    : loadGoCJ(input, scenario.taskCount());
        }
    }

    private static long[] loadGoCJ(InputStream input, int expectedTasks) throws IOException {
        List<Long> lengths = new ArrayList<>(expectedTasks);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            int lineNumber = 0;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String value = line.trim();
                if (value.isEmpty() || value.startsWith("#") || value.startsWith(";")) {
                    continue;
                }
                try {
                    long length = Long.parseLong(value);
                    if (length <= 0) {
                        throw new IOException("Non-positive GoCJ task length at line " + lineNumber);
                    }
                    lengths.add(length);
                } catch (NumberFormatException exception) {
                    throw new IOException("Invalid GoCJ task length at line " + lineNumber, exception);
                }
            }
        }

        if (lengths.size() != expectedTasks) {
            throw new IOException("Expected " + expectedTasks + " GoCJ tasks but found " + lengths.size());
        }
        return toArray(lengths);
    }

    private static long[] loadNasa(InputStream input, int expectedTasks) throws IOException {
        try (ZipInputStream zip = new ZipInputStream(input)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (!entry.isDirectory() && entry.getName().toLowerCase().endsWith(".swf")) {
                    return readNasaJobs(zip, expectedTasks);
                }
            }
        }
        throw new IOException("NASA workload archive does not contain an SWF file.");
    }

    private static long[] readNasaJobs(InputStream input, int expectedTasks) throws IOException {
        List<Long> lengths = new ArrayList<>(expectedTasks);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.US_ASCII))) {
            String line;
            int lineNumber = 0;
            while (lengths.size() < expectedTasks && (line = reader.readLine()) != null) {
                lineNumber++;
                String record = line.trim();
                if (record.isEmpty() || record.startsWith(";") || record.startsWith("#")) {
                    continue;
                }
                lengths.add(parseNasaLength(record, lineNumber));
            }
        }
        if (lengths.size() != expectedTasks) {
            throw new IOException("Expected " + expectedTasks + " NASA jobs but found " + lengths.size());
        }
        return toArray(lengths);
    }

    private static long parseNasaLength(String record, int lineNumber) throws IOException {
        String[] columns = record.split("\\s+");
        if (columns.length < 8) {
            throw new IOException("Incomplete NASA SWF record at line " + lineNumber);
        }

        try {
            double runtimeSeconds = Math.max(1, Double.parseDouble(columns[3]));
            double allocatedProcessors = Double.parseDouble(columns[4]);
            double requestedProcessors = Double.parseDouble(columns[7]);
            double processors = Math.max(1, Math.max(allocatedProcessors, requestedProcessors));
            double length = Math.ceil(runtimeSeconds * processors * 1000);
            if (!Double.isFinite(length) || length > Long.MAX_VALUE) {
                throw new IOException("NASA task length is out of range at line " + lineNumber);
            }
            return (long) length;
        } catch (NumberFormatException exception) {
            throw new IOException("Invalid NASA SWF record at line " + lineNumber, exception);
        }
    }

    private static long[] toArray(List<Long> values) {
        long[] result = new long[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = values.get(i);
        }
        return result;
    }
}
