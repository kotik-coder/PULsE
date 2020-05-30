package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;

public class WeighedScheme extends FixedStepIntegrator {

	private double weight;
	private final static double DEFAULT_WEIGHT = 0.5;
	
	public WeighedScheme(DiscreteIntensities intensities, EmissionFunction ef, IntegratedPhaseFunction ipf) {
		super(intensities, ef, ipf);
		weight = DEFAULT_WEIGHT;
	}
	
	public double stepRight(int i, int j) {
		return step(i, j, 1.0);
	}
	
	public double stepLeft(int i, int j) {
		return step(i, j, -1.0);
	}

	private double step(int i, int j, double sign) {
		final double h		= intensities.grid.step(j, sign);
		final double mu_h	= sign*intensities.mu[i]/h;
		return (source(i, j, intensities.grid.getNode(j)) + (mu_h - 1.0 + weight)*intensities.I[i][j])/(mu_h + weight);
	}
	
}