package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;

public abstract class AdaptiveIntegrator extends NumericIntegrator {

	protected final static double adaptiveErSq = 5e-4;
	private final static double DENSITY_FACTOR = 1.5;
		
	protected double[][] f;

	public AdaptiveIntegrator(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
	}

	public void reduceStepSize() {
		int nNew = (roundEven(DENSITY_FACTOR * intensities.grid.getDensity()));
		intensities.grid.generateUniform(nNew, true);
		this.intensities.reinitInternalArrays();
		intensities.clearBoundaryFluxes();
	}

	private int roundEven(double a) {
		return (int) (a / 2 * 2);
	}

}