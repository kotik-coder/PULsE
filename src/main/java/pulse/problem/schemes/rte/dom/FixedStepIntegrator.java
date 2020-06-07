package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;

public abstract class FixedStepIntegrator extends NumericIntegrator {

	public FixedStepIntegrator(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
	}

	@Override
	public void integrate() {
		final int N = intensities.grid.getDensity();
		
		final int nHalf = intensities.quadratureSet.getFirstNegativeNode();
		final int nStart = intensities.quadratureSet.getFirstPositiveNode();

		treatZeroIndex();
		
		/*
		 * First set of ODE. Initial condition corresponds to I(0) /t ----> tau0 The
		 * streams propagate in the positive hemisphere
		 */

		intensities.left(uExtended, emissionFunction); // initial value for tau = 0
		
		for (int j = 0, i = 0; j < N; j++) {

			for (i = nStart; i < nHalf; i++)
				intensities.I[i][j + 1] = stepRight(i, j);

		}

		/*
		 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
		 * streams propagate in the negative hemisphere
		 */

		intensities.right(uExtended, emissionFunction); // initial value for tau = tau_0

		for (int j = N, i = 0; j > 0; j--) {

			for (i = nHalf; i < intensities.n; i++)
				intensities.I[i][j - 1] = stepLeft(i, j);

		}

	}

	public abstract double stepLeft(int i, int j);

	public abstract double stepRight(int i, int j);

}