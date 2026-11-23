# Benchmark data sources

The GoCJ job-length files and the NASA Ames workload archive are from
[fawwazjs/Metaheuristic_JAYA_SA_Algorithm](https://github.com/fawwazjs/Metaheuristic_JAYA_SA_Algorithm/tree/main/cloudsim-jaya/data),
pinned to commit `c26539720d4ab0c8561c35aa38cb20ae79d5d6b6`.

- GoCJ: Hussain and Aleem (2018), DOI [10.3390/data3040038](https://doi.org/10.3390/data3040038).
  The dataset is distributed under CC BY 4.0; each row is a job length in MI.
- NASA Ames iPSC/860 1993: `NASA-iPSC-1993-3.1-cln.swf.zip`, sourced from the
  [Parallel Workloads Archive](https://www.cs.huji.ac.il/labs/parallel/workload/).
  The first 750 valid SWF jobs are used. Length is converted to MI as
  `max(runtime seconds, 1) * max(allocated processors, requested processors, 1) * 1000`.

The VM profile used for these scenarios is 500/800/1000/1300/1600 MIPS with
1/1/2/2/3 PEs, repeated in that order.
