package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.statements.ParticipatingMedium;

public abstract class NumericIntegrator {

	protected DiscreteIntensities intensities;

	public DiscreteIntensities getIntensities() {
		return intensities;
	}

	public void setIntensities(DiscreteIntensities intensities) {
		this.intensities = intensities;
	}

	protected EmissionFunction emissionFunction;

	public EmissionFunction getEmissionFunction() {
		return emissionFunction;
	}

	public void setEmissionFunction(EmissionFunction emissionFunction) {
		this.emissionFunction = emissionFunction;
	}

	protected PhaseFunction ipf;

	private double albedo;
	protected double[] uExtended;

	public NumericIntegrator(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		this.intensities = intensities;
		this.emissionFunction = ef;
		this.ipf = ipf;
	}

	public double getAlbedo() {
		return albedo;
	}

	public double[] getTemperatureArray() {
		return uExtended;
	}

	public void init(ParticipatingMedium problem, Grid grid) {
		setAlbedo((double) problem.getScatteringAlbedo().getValue());
		this.emissionFunction.init(problem);
		emissionFunction.setGridStep(grid.getXStep());
		intensities.setEmissivity((double) problem.getEmissivityProperty().getValue());
	}

	public abstract void integrate();

	public double rhs(int i, int j, double t, double intensity) {
		return 1.0 / intensities.mu[i] * (source(i, j, t) - intensity);
	}

	public void setAlbedo(double albedo) {
		this.albedo = albedo;
	}

	public void setTemperatureArray(double[] uExtended) {
		this.uExtended = uExtended;
	}

	public void treatZeroIndex() {

		if (intensities.quadratureSet.hasZeroNode()) {

			double denominator = 0;

			// loop through the spatial indices
			for (int j = 0; j < intensities.grid.getDensity() + 1; j++) {

				// solve I_k = S_k for mu[k] = 0
				denominator = 1.0 - 0.5 * albedo * intensities.w[0] * ipf.function(0, 0);
				intensities.I[0][j] = (sourceEmission(intensities.grid.getNode(j))
						+ 0.5 * albedo * ipf.integrateWithoutPoint(0, j, 0)) / denominator;

			}

		}

	}

	public double source(int i, int j, double t) {
		return sourceEmission(t) + 0.5 * albedo * ipf.integrate(i, j);
	}

	public double sourceEmission(double t) {
		double tau0 = intensities.grid.getDimension();
		return (1.0 - albedo) * emissionFunction.J(uExtended, t / tau0);
	}

}