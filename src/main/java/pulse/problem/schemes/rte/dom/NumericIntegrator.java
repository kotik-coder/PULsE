package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.statements.ParticipatingMedium;

public abstract class NumericIntegrator {

	protected DiscreteIntensities intensities;
	protected int nNegativeStart;
	protected int nPositiveStart;
	protected int nH;
	
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

	protected PhaseFunction pf;

	private double albedo;

	public NumericIntegrator(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		this.intensities = intensities;
		this.emissionFunction = ef;
		this.pf = ipf;
	}

	public double getAlbedo() {
		return albedo;
	}

	public void init(ParticipatingMedium problem, Grid grid) {
		setAlbedo((double) problem.getScatteringAlbedo().getValue());
		this.emissionFunction.init(problem);
		emissionFunction.setGridStep(grid.getXStep());
		intensities.setEmissivity((double) problem.getEmissivityProperty().getValue());
		nNegativeStart = intensities.quadratureSet.getFirstNegativeNode();
		nPositiveStart = intensities.quadratureSet.getFirstPositiveNode();
		nH = nNegativeStart - nPositiveStart;
	}

	public abstract void integrate();

	public void setAlbedo(double albedo) {
		this.albedo = albedo;
	}

	public void treatZeroIndex() {

		if (intensities.quadratureSet.hasZeroNode()) {

			double denominator = 0;

			// loop through the spatial indices
			for (int j = 0; j < intensities.grid.getDensity() + 1; j++) {

				// solve I_k = S_k for mu[k] = 0
				denominator = 1.0 - 0.5 * albedo * intensities.w[0] * pf.function(0, 0);
				intensities.I[j][0] = (emission(intensities.grid.getNode(j))
						+ 0.5 * albedo * pf.sumExcludingIndex(0, j, 0)) / denominator;

			}

		}

	}

	public double derivative(int i, int j, double t, double I) {
		return 1.0 / intensities.mu[i] * (source(i, j, t, I) - I);
	}

	public double derivative(int i, double t, double[] out, double[] in, int l1, int l2) {
		return 1.0 / intensities.mu[i] * (source(i, out, in, t, l1, l2) - out[i - l1]);
	}

	public double partial(int i, double t, double[] inward, int l1, int l2) {
		return (emission(t) + 0.5 * albedo * pf.inwardPartialSum(i, inward, l1, l2)) / intensities.mu[i];
	}

	public double partial(int i, int j, double t, int l1, int l2) {
		return (emission(t) + 0.5 * albedo * pf.partialSum(i, j, l1, l2)) / intensities.mu[i];
	}

	public double source(int i, int j, double t, double I) {
		return emission(t) + 0.5 * albedo * (pf.sumExcludingIndex(i, j, i) + pf.function(i, i) * intensities.w[i] * I); // contains
																														// sum
																														// over
																														// the
																														// incoming
																														// rays
	}

	public double source(int i, double[] iOut, double[] iIn, double t, int l1, int l2) {

		double sumOut = 0;

		for (int l = l1; l < l2; l++) // sum over the OUTWARD intensities iOut
			sumOut += iOut[l - l1] * intensities.w[l] * pf.function(i, l);

		double sumIn = 0;

		for (int start = intensities.n - l2, l = start, end = intensities.n - l1; l < end; l++) // sum over the INWARD
																								// intensities iIn
			sumIn += iIn[l - start] * intensities.w[l] * pf.function(i, l);

		return emission(t) + 0.5 * albedo * (sumIn + sumOut); // contains sum over the incoming rays

	}

	public double emission(double t) {
		return (1.0 - albedo) * emissionFunction.J(t);
	}

}