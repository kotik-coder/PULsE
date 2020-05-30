package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;

public abstract class FixedStepIntegrator extends NumericIntegrator {
	
	public FixedStepIntegrator(DiscreteIntensities intensities, EmissionFunction ef, IntegratedPhaseFunction ipf) {
		super(intensities, ef, ipf);
	}

	public abstract double stepRight(int i, int j);
	public abstract double stepLeft(int i, int j);
	
	@Override
	public void integrate() {
		final int N = intensities.grid.getDensity();
		final int n = intensities.n;

		/*
		 * First set of ODE. Initial condition corresponds to I(0)
		 * /t ----> tau0
		 * The streams propagate in the positive hemisphere
		 */
		
		intensities.left(uExtended, emissionFunction); //initial value for tau = 0
		
		for(int j = 0, i = 0; j < N; j++) {
		
			for(i = 0; i < n/2; i++) 
				intensities.I[i][j + 1] = stepRight(i, j);
			
		}
		
		/*
		 * Second set of ODE. Initial condition corresponds to I(tau0)
		 * /0 <---- t
		 * The streams propagate in the negative hemisphere
		 */		
		
		intensities.right(uExtended, emissionFunction); //initial value for tau = tau_0	
		
		for(int j = N, i = 0; j > 0; j--) {
			
			for(i = n/2; i < n; i++) 
				intensities.I[i][j - 1] = stepLeft(i, j);
			
		}		
		
	}
	
}