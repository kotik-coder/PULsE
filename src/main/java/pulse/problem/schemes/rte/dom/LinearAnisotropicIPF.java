package pulse.problem.schemes.rte.dom;

public class LinearAnisotropicIPF extends PhaseFunction {

	public LinearAnisotropicIPF(DiscreteIntensities intensities) {
		super(intensities);
	}

	@Override
	public double integratePartial(int i, int j, int n1, int n2Exclusive) {
		return intensities.g(j, n1, n2Exclusive) + A1 * intensities.mu[i] * intensities.q(j, n1, n2Exclusive);
	}

	@Override
	public double function(int i, int k) {
		return 1.0 + A1 * intensities.mu[i] * intensities.mu[k];
	}

}