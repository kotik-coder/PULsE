package pulse.problem.schemes.rte.dom;

public class LinearAnisotropicIPF extends IntegratedPhaseFunction {

	public LinearAnisotropicIPF(DiscreteIntensities intensities) {
		super(intensities);
	}

	@Override
	public double compute(int i, int j) {
		return intensities.g(j) + A1*intensities.mu[i]*intensities.q(j);
	}

}