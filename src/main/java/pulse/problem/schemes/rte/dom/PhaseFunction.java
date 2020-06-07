package pulse.problem.schemes.rte.dom;

public abstract class PhaseFunction {

	protected DiscreteIntensities intensities;
	protected double A1;

	public PhaseFunction(DiscreteIntensities intensities) {
		this.intensities = intensities;
	}

	public double integrate(int i, int j) {
		return integratePartial(i, j, 0, intensities.n);
	}

	public double integrateWithoutPoint(int i, int j, int index) {
		return integratePartial(i, j, 0, index) + integratePartial(i, j, index + 1, intensities.n);
	}

	public double integratePartial(int i, int j, int kStart, int kEndExclusive) {
		double result = 0;
		
		for (int k = kStart; k < kEndExclusive; k++) 
	 		result += intensities.w[k] * intensities.I[j][k] * function(i, k);
		return result;
	}
	
	public double integratePartial(int i, double I, int kStart, int kEndExclusive) {
		double result = 0;

		for (int k = kStart; k < kEndExclusive; k++)
			result += intensities.w[k] * I * function(i, k);
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