package pulse.problem.schemes.rte.dom;

import pulse.problem.statements.ParticipatingMedium;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.util.Reflexive;

public abstract class PhaseFunction implements Reflexive {

    private final Discretisation intensities;
    private double anisotropy;
    private double halfAlbedo;

    public PhaseFunction(ParticipatingMedium medium, Discretisation intensities) {
        this.intensities = intensities;
        init(medium);
    }

    public double fullSum(int i, int j) {
        return partialSum(i, j, 0, intensities.getOrdinates().getTotalNodes());
    }

    public double sumExcludingIndex(int i, int j, int index) {
        return partialSum(i, j, 0, index) + partialSum(i, j, index + 1, intensities.getOrdinates().getTotalNodes());
    }

    public double partialSum(int i, int j, int startInclusive, int endExclusive) {
        double result = 0;
        final var ordinates = intensities.getOrdinates();
        final var quantities = intensities.getQuantities();

        for (int k = startInclusive; k < endExclusive; k++) {
            result += ordinates.getWeight(k) * quantities.getIntensity(j, k) * function(i, k);
        }
        return result;
    }

    public double inwardPartialSum(int i, double[] inward, int kStart, int kEndExclusive) {
        double result = 0;
        final var ordinates = intensities.getOrdinates();

        for (int k = kStart; k < kEndExclusive; k++) {
            result += ordinates.getWeight(k) * inward[k - kStart] * function(i, k);
        }

        return result;
    }

    public abstract double function(int i, int k);

    public double getAnisotropyFactor() {
        return anisotropy;
    }

    protected Discretisation getDiscreteIntensities() {
        return intensities;
    }

    public void init(ParticipatingMedium problem) {
        var properties = (ThermoOpticalProperties) problem.getProperties();
        this.anisotropy = (double) properties.getScatteringAnisostropy().getValue();
        this.halfAlbedo = 0.5 * (double) properties.getScatteringAlbedo().getValue();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    public double getHalfAlbedo() {
        return halfAlbedo;
    }

}
