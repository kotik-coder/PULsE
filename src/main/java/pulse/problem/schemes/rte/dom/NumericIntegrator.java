package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.statements.ParticipatingMedium;
import pulse.search.math.Vector;

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

	public NumericIntegrator(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		this.intensities = intensities;
		this.emissionFunction = ef;
		this.ipf = ipf;
	}

	public double getAlbedo() {
		return albedo;
	}

	public void init(ParticipatingMedium problem, Grid grid) {
		setAlbedo((double) problem.getScatteringAlbedo().getValue());
		this.emissionFunction.init(problem);
		emissionFunction.setGridStep(grid.getXStep());
		intensities.setEmissivity((double) problem.getEmissivityProperty().getValue());
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
				denominator = 1.0 - 0.5 * albedo * intensities.w[0] * ipf.function(0, 0);
				intensities.I[j][0] = (sourceEmission(intensities.grid.getNode(j))
						+ 0.5 * albedo * ipf.integrateWithoutPoint(0, j, 0)) / denominator;

			}

		}

	}
	
	public double rhs(int i, int j, double t, double I) {	
		return 1.0 / intensities.mu[i] * ( source(i, j, t, I) - I );
	}
	
	public double rhs(int i, int j, double t, double[] outwardIntensities, double sign) {	
		
		final int nHalf		= intensities.quadratureSet.getFirstNegativeNode();
		final int nStart	= intensities.quadratureSet.getFirstPositiveNode();
		final int l1 = sign > 0 ? nStart : nHalf;			//either first positive index or first negative (n/2)
		final int l2 = sign > 0 ? nHalf : intensities.n;	//either first negative index (n/2) or n
		
		return 1.0 / intensities.mu[i] * ( 
				source(i, j, outwardIntensities, t, l1, l2) 
				- outwardIntensities[i - l1] );
		
	}
	
	public double source(int i, int j, double t, double I) {
		return sourceEmission(t) + 0.5 * albedo * ( ipf.integrateWithoutPoint(i, j, i) + 
				ipf.function(i, i) * intensities.w[i] * I ); //contains sum over the incoming rays	
	}

	public double source(int i, int j, double[] iOut, double t, int l1, int l2) {
		
		double sumOut = 0;
		
		for(int l = l1; l < l2; l++)		//sum over the outward intensities iOut
			sumOut += iOut[l - l1] * intensities.w[l] * ipf.function(i, l);
		
		int l3 = intensities.n - l2; //either nHalf or nStart
		int l4 = intensities.n - l1; //either n or nHalf
		
		return sourceEmission(t) + 0.5 * albedo * ( ipf.integratePartial(i, j, l3, l4) + sumOut ); //contains sum over the incoming rays
	
	}

	public double sourceEmission(double t) {
		return (1.0 - albedo) * emissionFunction.J(t);
	}

}