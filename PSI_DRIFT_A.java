// Landmark Approach for drift detecntion with PSI based detector

package moa.classifiers.core.driftdetection;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.FloatOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import java.util.*;

public class PSI_DRIFT_A extends AbstractChangeDetector {

    private static final long serialVersionUID = -3518369648142099719L;

    public IntOption minNumInstancesOption = new IntOption(
            "minNumInstances",
            'n',
            "The number of instances to wait before beginning detection.",
            100, 0, Integer.MAX_VALUE);

    public IntOption binsOption = new IntOption(
            "bins",
            'b',
            "Number of bins for PSI computation.",
            10, 1, Integer.MAX_VALUE);

    public IntOption instancesPerDistributionOption = new IntOption(
            "instancesPerDistribution",
            'i',
            "Number of instances per distribution for comparison.",
            100, 1, Integer.MAX_VALUE);

    public FloatOption warningLevelOption = new FloatOption(
            "warningLevel", 'w', "Warning Level.",
            0.10, 0.0, 1.0);

    public FloatOption outcontrolLevelOption = new FloatOption(
            "outcontrolLevel", 'o', "Outcontrol Level.",
            0.20, 0.0, 1.0);

    private int m_n;
    private int minNumInstances;
    private int numBins;
    private int instancesPerDistribution;
    private double warningLevel;
    private double outcontrolLevel;

    private LinkedList<Double> firstWindow;
    private LinkedList<Double> secondWindow;

    public PSI_DRIFT_A() {
        resetLearning();
    }

    @Override
    public void resetLearning() {
        m_n = 0;
        minNumInstances = this.minNumInstancesOption.getValue();
        numBins = this.binsOption.getValue();
        instancesPerDistribution = this.instancesPerDistributionOption.getValue();
        warningLevel = this.warningLevelOption.getValue();
        outcontrolLevel = this.outcontrolLevelOption.getValue();
        firstWindow = new LinkedList<>();
        secondWindow = new LinkedList<>();
    }

    @Override
    public void input(double prediction) {
        m_n++;

        if (m_n <= minNumInstances) {
            return;
        }

        if (m_n <= minNumInstances + instancesPerDistribution) {
            firstWindow.add(prediction);
            return;
        }

        secondWindow.add(prediction);

        if (secondWindow.size() < instancesPerDistribution) {
            return;
        }

        double psi = computePSI(firstWindow, secondWindow, numBins);

        updateDetectionStatus(psi);

        firstWindow = secondWindow;
        secondWindow = new LinkedList<>();
    }

    private void updateDetectionStatus(double psi) {
        this.estimation = psi;
        this.isChangeDetected = false;
        this.isWarningZone = false;
        this.delay = 0;
        if (psi > outcontrolLevel) {
            this.isChangeDetected = true;
        } else if (psi > warningLevel) {
            this.isWarningZone = true;
        }
    }

    private double computePSI(List<Double> expected, List<Double> actual, int numBins) {
        double minValue = Collections.min(expected);
        double maxValue = Collections.max(expected);
        double binWidth = (maxValue - minValue) / numBins;

        double[] expectedDist = new double[numBins];
        double[] actualDist = new double[numBins];
        for (Double value : expected) {
            int binIndex = Math.min((int) ((value - minValue) / binWidth), numBins - 1);
            expectedDist[binIndex] += 1;
        }
        for (Double value : actual) {
            int binIndex = Math.min((int) ((value - minValue) / binWidth), numBins - 1);
            actualDist[binIndex] += 1;
        }

        double psi = 0.0;
        for (int i = 0; i < numBins; i++) {
            expectedDist[i] /= expected.size();
            actualDist[i] /= actual.size();
            if (expectedDist[i] > 0.0 && actualDist[i] > 0.0) {
                psi += (expectedDist[i] - actualDist[i]) * Math.log(expectedDist[i] / actualDist[i]);
            }
        }

        return psi;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // Append the description of the drift detection algorithm to the StringBuilder
       
        StringBuilder newLineIndent = new StringBuilder();
        for (int i = 0; i < indent; i++) {
            newLineIndent.append("\n");
        }
        sb.append(newLineIndent).append("PSI_DRIFT_A: Detects concept drift based on the PSI measure.");
        sb.append(newLineIndent).append("Parameters:");
        sb.append(newLineIndent).append("- minNumInstances: The number of instances to wait before beginning detection.");
        sb.append(newLineIndent).append("- bins: Number of bins for PSI computation.");
        sb.append(newLineIndent).append("- instancesPerDistribution: Number of instances per distribution for comparison.");
        sb.append(newLineIndent).append("- warningLevel: Warning level for drift detection.");
        sb.append(newLineIndent).append("- outcontrolLevel: Outcontrol level for drift detection.");
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor,
                                      ObjectRepository repository) {
    }
}

