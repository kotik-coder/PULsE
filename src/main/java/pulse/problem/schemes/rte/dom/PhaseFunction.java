package pulse.problem.schemes.rte.dom;

public abstract class PhaseFunction {

	protected DiscreteIntensities intensities;
	protected double A1;

	public PhaseFunction(DiscreteIntensities intensities) {
		this.intensities = intensities;
	}

	public double fullSum(int i, int j) {
		return partialSum(i, j, 0, intensities.n);
	}

	public double sumExcludingIndex(int i, int j, int index) {
		return partialSum(i, j, 0, index) + partialSum(i, j, index + 1, intensities.n);
	}

	public double partialSum(int i, int j, int startInclusive, int endExclusive) {
		double result = 0;

		for (int k = startInclusive; k < endExclusive; k++)
			result += intensities.w[k] * intensities.I[j][k] * function(i, k);
		return result;
	}

	public double inwardPartialSum(int i, double[] inward, int kStart, int kEndExclusive) {
		double result = 0;

		for (int k = kStart; k < kEndExclusive; k++)
			result += intensities.w[k] * inward[k - kStart] * function(i, k);

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

}