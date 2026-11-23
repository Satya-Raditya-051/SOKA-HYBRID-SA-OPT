package com.example;

public enum Scenario {
    GOCJ_500_TASKS_50_VMS(
            "GoCJ 500 Task x 50 VM", 500, 50, "/data/gocj/GoCJ_Dataset_500.txt", false),
    GOCJ_1000_TASKS_50_VMS(
            "GoCJ 1000 Task x 50 VM", 1000, 50, "/data/gocj/GoCJ_Dataset_1000.txt", false),
    GOCJ_750_TASKS_10_VMS(
            "GoCJ 750 Task x 10 VM", 750, 10, "/data/gocj/GoCJ_Dataset_750.txt", false),
    NASA_750_JOBS_50_VMS(
            "NASA Ames 750 Job x 50 VM", 750, 50, "/data/nasa/NASA-iPSC-1993-3.1-cln.swf.zip", true);

    private final String label;
    private final int taskCount;
    private final int vmCount;
    private final String resourcePath;
    private final boolean nasaWorkload;

    Scenario(String label, int taskCount, int vmCount, String resourcePath, boolean nasaWorkload) {
        this.label = label;
        this.taskCount = taskCount;
        this.vmCount = vmCount;
        this.resourcePath = resourcePath;
        this.nasaWorkload = nasaWorkload;
    }

    public String label() {
        return label;
    }

    public int taskCount() {
        return taskCount;
    }

    public int vmCount() {
        return vmCount;
    }

    String resourcePath() {
        return resourcePath;
    }

    boolean isNasaWorkload() {
        return nasaWorkload;
    }
}
