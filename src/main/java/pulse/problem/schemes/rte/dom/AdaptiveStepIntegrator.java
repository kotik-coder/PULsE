package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;

public abstract class AdaptiveStepIntegrator extends NumericIntegrator {

	private final static double adaptiveErSq = 5e-4;
	private final static double DENSITY_FACTOR = 1.5;
	
	public AdaptiveStepIntegrator(DiscreteIntensities intensities, EmissionFunction ef, IntegratedPhaseFunction ipf) {
		super(intensities, ef, ipf);
	}

	public abstract double[] stepRight(int i, int j);
	public abstract double[] stepLeft(int i, int j);
	
	@Override
	public void integrate() {
		double[] array;
		int N = intensities.grid.getDensity();
		int halfN = N/2;
		
		/*
		 * First set of ODE. Initial condition corresponds to I(0)
		 * /t ----> tau0
		 * The streams propagate in the positive hemisphere
		 */
			
		outer: for(	double erSq = 1.0 ; erSq > adaptiveErSq ; N = intensities.grid.getDensity(), halfN = N/2 ) {
			
			erSq = 0;
			intensities.left(uExtended, emissionFunction); //initial value for tau = 0
			
			for(int j = 0, i = 0; j < N; j++) {
			
				for(i = 0; (i < halfN) && (erSq < adaptiveErSq); i++) { 
					array = stepRight(i, j);
					intensities.I[i][j + 1] = array[0];
					erSq = array[1];						
				}
				
			}
			
			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0)
			 * /0 <---- t
			 * The streams propagate in the negative hemisphere
			 */		
			
			intensities.right(uExtended, emissionFunction); //initial value for tau = tau_0	
			
			for(int j = N, i = 0; j > 0; j--) {
				
				for(i = halfN; (i < N ) && (erSq < adaptiveErSq); i++) { 
					array = stepLeft(i, j);
					intensities.I[i][j - 1] = array[0];
					erSq = array[1];				
				}
				
			}
			
			if(erSq < adaptiveErSq)
				break outer;
			else 
				reduceStepSize();
			
		}
		
	}
	
	private int roundEven(double a) {
		return (int) (a/2 * 2);
	}
	
	private void reduceStepSize() {
		int nNew = (int)( roundEven( DENSITY_FACTOR*intensities.grid.getDensity() ) );
		intensities.grid.generateUniform(nNew, true);
		this.intensities.reinitInternalArrays();
	}

}