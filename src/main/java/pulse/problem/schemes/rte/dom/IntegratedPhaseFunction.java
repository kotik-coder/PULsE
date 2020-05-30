package pulse.problem.schemes.rte.dom;

public abstract class IntegratedPhaseFunction {

	protected DiscreteIntensities intensities;
	protected double A1;
	
	public IntegratedPhaseFunction(DiscreteIntensities intensities) {
		this.intensities = intensities;
	}
	
	public abstract double compute(int i, int j);

	public DiscreteIntensities getDiscreteIntensities() {
		return intensities;
	}

	public void setDiscreteIntensities(DiscreteIntensities moments) {
		this.intensities = moments;
	}
	
	public double getAnisotropyFactor() {
		return A1;
	}

	public void setAnisotropyFactor(double a1) {
		A1 = a1;
	}

}