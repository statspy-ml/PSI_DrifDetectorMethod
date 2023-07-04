
# VERSION INFORMATION

PSI_DRIFT.java is deprecated

PSI_DRIFT_A.java -> Landmark Windows

PSI_DRIFT_B.java -> Sliding Windows



# POPULATION STABILITY INDEX DETECTOR METHOD

*m_n* : counts number of instances

*minNumInstances* : grace_period -  Number of skipped instances before starts the drift detector

*numBins* :  sets the number of bins in each window distributions. Defaults to 10 bins.

*instancesPerDistribution* : sets the nuber of instances per distributions windows. Defaults to 100 instances.

*warningLevel* :  sets the threshold to trigger warning alarm. Defaults to 10%

*outcontrolLevel* : sets the threshold to trigger drift detection.  Defaults to 20%.


# HOW THE PSI DRIFT DETECTOR METHOD WORKS

## i.Sliding Window

1. Two LinkedLists, `firstWindow` and `secondWindow`, are used to store the instances of the current and previous distributions.

2. The `resetLearning()` method initializes the member variables to their default values.

3. The `input(double prediction)` method is called for each new prediction in the data stream. It increments the instance count (`m_n`) and performs the following steps:

   a. If the instance count is less than or equal to the number of instances per distribution, the prediction is added to the `firstWindow` and the method returns.

   b. If the instance count exceeds the number of instances per distribution, the prediction is added to the `secondWindow`.

   c. If the size of the `secondWindow` is less than the number of instances per distribution, the method returns.

   d. The `computePSI()` method is called to calculate the PSI between the `firstWindow` and `secondWindow`.

   e. The `updateDetectionStatus()` method updates the detection status based on the calculated PSI value.

   f. The oldest instance is removed from the `firstWindow`, and the first instance from the `secondWindow` is added to the `firstWindow`.

4. The `computePSI(List<Double> expected, List<Double> actual, int numBins)` method calculates the PSI measure between two distributions (`expected` and `actual`) using the specified number of bins. It computes the histogram of each distribution, normalizes the histograms, and then calculates the PSI value based on the normalized histograms.

5. The `updateDetectionStatus(double psi)` method updates the detection status based on the calculated PSI value. It sets the `estimation` field to the PSI value, determines if a change or warning is detected based on the warning and outcontrol levels, and updates the corresponding fields (`isChangeDetected`, `isWarningZone`, and `delay`).


