package pulse.problem.schemes.rte.dom;

import pulse.problem.statements.ParticipatingMedium;
import pulse.util.Reflexive;

public abstract class PhaseFunction implements Reflexive {

	protected DiscreteIntensities intensities;
	protected double A1;

	public PhaseFunction(ParticipatingMedium medium, DiscreteIntensities intensities) {
		this.intensities = intensities;
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

		for (int k = startInclusive; k < endExclusive; k++) {
			result += ordinates.getWeight(k) * intensities.getIntensity(j, k) * function(i, k);
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
		return A1;
	}

	public DiscreteIntensities getDiscreteIntensities() {
		return intensities;
	}

	public void setAnisotropyFactor(double a1) {
		A1 = a1;
	}

	public void setDiscreteIntensities(DiscreteIntensities moments) {
		this.intensities = moments;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName();
	}

}