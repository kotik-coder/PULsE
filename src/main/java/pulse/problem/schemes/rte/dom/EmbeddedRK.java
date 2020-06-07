package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Vector;

public class EmbeddedRK extends AdaptiveIntegrator {

	private ButcherTableau tableau;
	
	public EmbeddedRK(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		tableau = ButcherTableau.DEFAULT_TABLEAU;
	}
	
	@Override
	public void integrate() {
		double[] array;

		int N = intensities.grid.getDensity();
		final int nHalf = intensities.quadratureSet.getFirstNegativeNode();
		final int nStart = intensities.quadratureSet.getFirstPositiveNode();
		
		outer: for (double erSq = 1.0; erSq > adaptiveErSq; N = intensities.grid.getDensity()) {

			erSq = 0;

			treatZeroIndex();
			
			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(uExtended, emissionFunction); // initial value for tau = 0
			
			for (int j = 0, i; j < N && -erSq < adaptiveErSq; j++) {

				for (i = nStart; (i < nHalf) ; i++) {
					array = rk(i, j, 1.0);
					intensities.I[i][j + 1] = array[0];
					erSq = Math.max( erSq, array[1] * array[1] );
				}

			}

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(uExtended, emissionFunction); // initial value for tau = tau_0
			
			for (int j = N, i = 0; j > 0 && -erSq < adaptiveErSq; j--) {

				for (i = nHalf; (i < intensities.n) ; i++) {
					array = rk(i, j, -1.0);
					intensities.I[i][j - 1] = array[0];
					erSq = Math.max( erSq, array[1] * array[1] );
				}

			}
			
			System.out.printf("%n%3.7f %5d", Math.sqrt(erSq), N);
			
			if (erSq < adaptiveErSq)
				break outer;
			else
				reduceStepSize();

		}

	}

	private double[] rk(int i, int j, final double sign) {

		double h			 = intensities.grid.step(j, sign);
		final double hSigned = h * sign;
		final double t		 = intensities.grid.getNode(j);
		double[] q			 = new double[tableau.stages()];
		
		double sum		= 0;
		double error	= 0;
	
		for (int m = 0; m < q.length; m++) {

			sum = 0;
			for (int k = 0; k < m; k++)
				sum += tableau.coefs.get(m, k) * q[k];

			q[m] = rhs(i, j, t + hSigned * tableau.c.get(m), intensities.I[i][j] + sum * hSigned);

			error += (tableau.b.get(m) - tableau.bHat.get(m)) * q[m];
			
		}
		
		return new double[] { intensities.I[i][j] + hSigned * tableau.b.dot(new Vector(q)), error * hSigned };

	}

	public ButcherTableau getButcherTableau() {
		return tableau;
	}

	public void setButcherTableau(ButcherTableau coef) {
		this.tableau = coef;
	}

}