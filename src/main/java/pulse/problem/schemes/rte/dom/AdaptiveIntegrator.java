package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Vector;

public abstract class AdaptiveIntegrator extends NumericIntegrator {
	
	protected final static double rtol = 1e-2;
	
	protected final static double DENSITY_FACTOR = 1.5;

	protected double[][] f;
	protected double[] qLast;

	protected boolean firstRun;

	private double rtolSq;
	
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
		rtolSq	= rtol*rtol;
		
		for (double erSq = 1.0, relFactor = 0.0, i0Max = 0; erSq > relFactor * rtolSq; N = intensities.grid.getDensity()) {

			erSq = 0;
			f = new double[N + 1][intensities.n]; // first index - spatial steps, second index - quadrature points

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(emissionFunction); // initial value for tau = 0
			i0Max = (new Vector(intensities.I[0])).maxSqComponent();
			
			relFactor = (new Vector(intensities.I[0])).maxSqComponent();
			firstRun = true;

			for (int j = 0; j < N && erSq < relFactor * rtolSq; j++) {
				v = step(j, 1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j + 1], nStart, nHalf - nStart);
				
				relFactor	= Math.max( i0Max, (new Vector(intensities.I[j + 1])).maxSqComponent() ); 
				erSq		= v[1].maxSqComponent();
			}

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(emissionFunction); // initial value for tau = tau_0
			i0Max = (new Vector(intensities.I[N])).maxSqComponent();
			
			relFactor = ( new Vector(intensities.I[N] )).lengthSq();
			firstRun = true;
			
			for (int j = N; j > 0 && erSq < relFactor * rtolSq; j--) {
				v = step(j, -1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j - 1], nHalf, nHalf - nStart);
				
				relFactor	= Math.max( i0Max, (new Vector(intensities.I[j - 1])).maxSqComponent() ); 
				erSq		= v[1].maxSqComponent();
			}

			System.out.printf("%n Steps: %5d Error: %1.5e, h = %3.6f", N, erSq, intensities.grid.stepRight(0));

			if (erSq > relFactor * rtolSq) {
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