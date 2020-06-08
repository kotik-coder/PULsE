package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Vector;

public abstract class AdaptiveIntegrator extends NumericIntegrator {

	protected final static double rtolSq = 1e-4;
	private final static double DENSITY_FACTOR = 1.5;
		
	protected double[][] f;

	@Override
	public void integrate() {
		Vector[] v;

		int N				= intensities.grid.getDensity();
		final int nHalf		= intensities.quadratureSet.getFirstNegativeNode();
		final int nStart	= intensities.quadratureSet.getFirstPositiveNode();

		for ( double erSq = 1.0, iSq = 0.0; erSq > rtolSq; N = intensities.grid.getDensity() ) {

			erSq = 0;
			f = new double[N + 1][intensities.n];

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(emissionFunction); // initial value for tau = 0
			iSq = ( intensities.I[0][nStart] * intensities.I[0][nStart] );
			
			for (int j = 0; j < N && erSq < rtolSq; j++) {
				v = step(j, 1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j + 1], nStart, nHalf - nStart);
				erSq = v[1].lengthSq() / iSq;
			}

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(emissionFunction); // initial value for tau = tau_0
			iSq = ( intensities.I[N][nHalf] * intensities.I[N][nHalf] );
			
			for (int j = N; j > 0 && erSq < rtolSq; j--) {

				v = step(j, -1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j - 1], nHalf, nHalf - nStart);
				erSq = v[1].lengthSq() / iSq;

			}

			System.out.printf("%n%5d %3.7f", N, erSq);
			
			if (erSq > rtolSq) {
				reduceStepSize();
				f = new double[0][0]; //clear derivatives
				HermiteInterpolator.clear();
			}
				
		}

	}
	
	public abstract Vector[] step(final int j, final double sign);
	
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