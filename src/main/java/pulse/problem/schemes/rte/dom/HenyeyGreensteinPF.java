package pulse.problem.schemes.rte.dom;

import pulse.problem.statements.ParticipatingMedium;

public class HenyeyGreensteinPF extends PhaseFunction {

	private double a1;
	private double a2;
	private double b1;

	public HenyeyGreensteinPF(ParticipatingMedium medium, DiscreteIntensities intensities) {
		super(medium, intensities);
		init();
	}

	public void init() {
		b1 = 2.0 * getAnisotropyFactor();
		a1 = 1.0 - getAnisotropyFactor() * getAnisotropyFactor();
		a2 = 1.0 + getAnisotropyFactor() * getAnisotropyFactor();
	}

	public double function(int i, int k) {
		double theta = intensities.ordinates.mu[k] * intensities.ordinates.mu[i];
		double f = a2 - b1 * theta;
		return a1 / (f * Math.sqrt(f));
	}

	@Override
	public void setAnisotropyFactor(double A1) {
		super.setAnisotropyFactor(A1);
		init();
	}

}