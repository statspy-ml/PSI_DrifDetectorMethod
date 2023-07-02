package moa.classifiers.core.driftdetection;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.FloatOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PSI_DRIFT extends AbstractChangeDetector {

    private static final long serialVersionUID = -3518369648142099719L;

    public IntOption windowSizeOption = new IntOption(
            "windowSize",
            'w',
            "The size of the window in number of instances.",
            100, 1, Integer.MAX_VALUE);

    public FloatOption warningLevelOption = new FloatOption(
            "warningLevel", 'l', "Warning Level.",
            0.10, 0.0, 1.0);

    public FloatOption outcontrolLevelOption = new FloatOption(
            "outcontrolLevel", 'o', "Outcontrol Level.",
            0.20, 0.0, 1.0);

    private int windowSize;
    private List<Double> windowA;
    private List<Double> windowB;

    private double warningLevel;
    private double outcontrolLevel;

    public PSI_DRIFT() {
        resetLearning();
    }

    @Override
    public void resetLearning() {
        windowSize = this.windowSizeOption.getValue();
        warningLevel = this.warningLevelOption.getValue();
        outcontrolLevel = this.outcontrolLevelOption.getValue();
        windowA = new ArrayList<>();
        windowB = new ArrayList<>();
    }

    @Override
    public void input(double prediction) {
        if (windowA.size() < windowSize) {
            windowA.add(prediction);
        } else if (windowB.size() < windowSize) {
            windowB.add(prediction);
        } else {
            double psi = computePSI(windowA, windowB);

            this.estimation = psi;
            this.isChangeDetected = false;
            this.isWarningZone = false;
            this.delay = 0;

            if (psi > outcontrolLevel) {
                this.isChangeDetected = true;
            } else if (psi > warningLevel) {
                this.isWarningZone = true;
            }

            // RESET e SWAP
            windowA = windowB;
            windowB = new ArrayList<>();
            windowB.add(prediction);
        }
    }

    private double computePSI(List<Double> expected, List<Double> actual) {
        int numBins = 10;
        double minValue = Collections.min(expected);
        double maxValue = Collections.max(expected);
        double binWidth = (maxValue - minValue) / numBins;

        
        double[] expectedDist = new double[numBins];
        double[] actualDist = new double[numBins];
        for (double val : expected) {
            int binIndex = Math.min((int) ((val - minValue) / binWidth), numBins - 1);
            expectedDist[binIndex]++;
        }
        for (double val : actual) {
            int binIndex = Math.min((int) ((val - minValue) / binWidth), numBins - 1);
            actualDist[binIndex]++;
        }

        // Normalização
        for (int i = 0; i < numBins; i++) {
            expectedDist[i] /= expected.size();
            actualDist[i] /= actual.size();
        }

        // Cálculo PSI
        double psi = 0;
        for (int i = 0; i < numBins; i++) {
            if (expectedDist[i] != 0) {
                double actualRate = actualDist[i] == 0 ? 0.001 : actualDist[i]; // Avoid division by zero
                psi += (expectedDist[i] - actualRate) * Math.log(expectedDist[i] / actualRate);
            }
        }

        return psi;
    }

    @Override
    public void getDescription(StringBuilder sb, int indent) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void prepareForUseImpl(TaskMonitor monitor, ObjectRepository repository) {
        // TODO Auto-generated method stub
    }
}

