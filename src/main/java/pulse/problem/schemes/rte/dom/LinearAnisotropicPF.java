package pulse.problem.schemes.rte.dom;

public class LinearAnisotropicPF extends PhaseFunction {

	private double g;
	
	public LinearAnisotropicPF(DiscreteIntensities intensities) {
		super(intensities);
		g = 3.0 * A1;
	}

	@Override
	public double partialSum(int i, int j, int n1, int n2Exclusive) {
		return intensities.g(j, n1, n2Exclusive) + g * intensities.ordinates.mu[i] * intensities.q(j, n1, n2Exclusive);
	}

	@Override
	public double function(int i, int k) {
		return 1.0 + g * intensities.ordinates.mu[i] * intensities.ordinates.mu[k];
	}
	
	@Override
	public void setAnisotropyFactor(double a1) {
		super.setAnisotropyFactor(a1);
		g = 3.0 * A1;
	}

}