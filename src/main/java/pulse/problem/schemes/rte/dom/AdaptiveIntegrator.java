package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Vector;

public abstract class AdaptiveIntegrator extends NumericIntegrator {
	
	protected final static double rtol = 1e-2;
	protected final static double atol = 1e-3;
	
	protected final static double DENSITY_FACTOR = 1.5;

	protected double[][] f;
	protected double[] qLast;

	protected boolean firstRun;

	public boolean isFirstRun() {
		return firstRun;
	}

	@Override
	public void integrate() {
		Vector[] v;

		int N = intensities.grid.getDensity();
		final int nHalf = intensities.quadratureSet.getFirstNegativeNode();
		final int nStart = intensities.quadratureSet.getFirstPositiveNode();

		qLast	= new double[intensities.n];
		
		for (double error = 1.0, relFactor = 0.0, i0Max = 0, i1Max = 0; error > atol + relFactor * rtol; N = intensities.grid.getDensity()) {

			error = 0;
			f = new double[N + 1][intensities.n]; // first index - spatial steps, second index - quadrature points

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(emissionFunction); // initial value for tau = 0
			i0Max = (new Vector(intensities.I[0])).maxAbsComponent();
			
			firstRun = true;

			for (int j = 0; j < N && error < atol + relFactor * rtol; j++) {
				
				v = step(j, 1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j + 1], nStart, nHalf - nStart);
				
				i1Max		= (new Vector(intensities.I[j + 1])).maxAbsComponent();
				relFactor	= Math.max( i0Max, i1Max );
				i0Max		= i1Max;
				
				error		= v[1].maxAbsComponent();
			
			}

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(emissionFunction); // initial value for tau = tau_0
			i0Max = (new Vector(intensities.I[N])).maxAbsComponent();
			
			firstRun = true;
			
			for (int j = N; j > 0 && error < atol + relFactor * rtol; j--) {
				
				v = step(j, -1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j - 1], nHalf, nHalf - nStart);
				
				i1Max		= (new Vector(intensities.I[j - 1])).maxAbsComponent();
				relFactor	= Math.max( i0Max, i1Max );
				i0Max		= i1Max;
				
				error		= v[1].maxAbsComponent();
			
			}

			System.out.printf("%n Steps: %5d Error: %1.5e, h = %3.6f", N, error, intensities.grid.stepRight(0));

			if (error > atol + relFactor * rtol) {
				reduceStepSize();
				f = new double[0][0]; //clear derivatives
				HermiteInterpolator.clear();
			}

		}
		
		System.exit(1);

	}

	public abstract Vector[] step(final int j, final double sign);

	public AdaptiveIntegrator(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
	}

	public void reduceStepSize() {
		int nNew = (roundEven(DENSITY_FACTOR * intensities.grid.getDensity()));
		generateGrid(nNew);
		this.intensities.reinitInternalArrays();
		intensities.clearBoundaryFluxes();
	}

	public void generateGrid(int nNew) {
		intensities.grid.generateUniform(nNew, true);
	}

	private int roundEven(double a) {
		return (int) (a / 2 * 2);
	}

}