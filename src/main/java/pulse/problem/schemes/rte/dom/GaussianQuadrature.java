package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.MathUtils;

public class GaussianQuadrature extends LegendrePoly {

	public GaussianQuadrature(final int n) {
		super(n);
		roots = new double[n];
		weights = new double[n];
	}
	
	@Override
	public void init() {
		coefficients();
		gaussianNodes();
		gaussianWeights();	
	}
	
	/**
	 * Calculates the Gaussian weights.
	 * Uses the formula by Abramowitz & Stegun (Abramowitz & Stegun 1972, p. 887))
	 */
	
	private void gaussianWeights() {
		double denominator = 1;
		
		for(int i = 0; i < roots.length; i++) {
			denominator = (1 - roots[i]*roots[i])*MathUtils.fastPowLoop(derivative(roots[i]), 2);
			weights[i] = 2.0/denominator;
		}
			
	}
	
	private void gaussianNodes() {
		var complexRoots = solver.solveAllComplex(c, 1.0);

		//the last roots is always zero, so we have n non-zero roots in total
		//in case of even n, the first n/2 roots are positive and the rest are negative
		for(int i = 0; i < complexRoots.length; i++) 
			roots[i] = complexRoots[i].getReal();
			
	}
		
	@Override
	public String getPrefix() {
		return "Gauss-Legendre Quadrature";
	}
	
}