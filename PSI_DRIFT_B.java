package moa.classifiers.core.driftdetection;

import com.github.javacliparser.IntOption;
import com.github.javacliparser.FloatOption;
import moa.core.ObjectRepository;
import moa.tasks.TaskMonitor;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;

public class PSI_DRIFT extends AbstractChangeDetector {

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
    private List<Double> window;
    private double warningLevel;
    private double outcontrolLevel;

    public PSI_DRIFT() {
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
        window = new ArrayList<>();
    }

    @Override
    public void input(double prediction) {
        if(m_n++ < minNumInstances) {
            return;
        }

        window.add(prediction);

        if (window.size() < 2 * instancesPerDistribution) {
            return;
        }

        // Calcula PSI
        List<Double> expected = window.subList(0, instancesPerDistribution);
        List<Double> actual = window.subList(window.size() - instancesPerDistribution, window.size());

        double psi = computePSI(expected, actual, numBins);

        // Warning e drift 
        this.estimation = psi;
        this.isChangeDetected = false;
        this.isWarningZone = false;
        this.delay = 0;
        if (psi > outcontrolLevel) {
            this.isChangeDetected = true;
        } else if (psi > warningLevel) {
            this.isWarningZone = true;
        }

        // Remove instance mais antiga da janela deslizante
        window.remove(0);
    }

    private double computePSI(List<Double> expected, List<Double> actual, int numBins) {
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
        
        // Normalizando as distribuições
        for (int i = 0; i < numBins; i++) {
            expectedDist[i] /= expected.size();
            actualDist[i] /= actual.size();
        }
        
        // Cálculo PSI
        double psi = 0;
        for (int i = 0; i < numBins; i++) {
            if (expectedDist[i] != 0) {
                double actualRate = actualDist[i] == 0 ? 0.001 : actualDist[i];  // Avoid division by zero
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

