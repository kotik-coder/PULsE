package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Vector;

public class EmbeddedRK extends AdaptiveIntegrator {

	private ButcherTableau tableau;
	private int nStart;
	private int nHalf;
	
	public EmbeddedRK(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		tableau	= ButcherTableau.DEFAULT_TABLEAU;
		nHalf	= intensities.quadratureSet.getFirstNegativeNode();
		nStart	= intensities.quadratureSet.getFirstPositiveNode();
	}

	@Override
	public void integrate() {
		Vector[] v;

		int N				= intensities.grid.getDensity();
		final int nHalf		= intensities.quadratureSet.getFirstNegativeNode();
		final int nStart	= intensities.quadratureSet.getFirstPositiveNode();

		outer: for (double erSq = 1.0; erSq > adaptiveErSq; N = intensities.grid.getDensity()) {

			erSq = 0;
			f = new double[intensities.n][N + 1];

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			intensities.left(emissionFunction); // initial value for tau = 0

			for (int j = 0; j < N && erSq < adaptiveErSq; j++) {
			
				v = rk(j, 1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j + 1], nStart, nHalf - nStart);
				erSq = v[1].lengthSq();

			}

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */

			intensities.right(emissionFunction); // initial value for tau = tau_0

			for (int j = N; j > 0 && erSq < adaptiveErSq; j--) {

				v = rk(j, -1.0);
				System.arraycopy(v[0].getData(), 0, intensities.I[j - 1], nHalf, nHalf - nStart);
				erSq = v[1].lengthSq();

			}

			System.out.printf("%n%5d %3.7f", N, erSq);
			
			if (erSq < adaptiveErSq)
				break outer;
			else {
				reduceStepSize();
				f = new double[0][0];
			}
				
		}

	}

	private Vector[] rk(int j, final double sign) {

		final double h			= intensities.grid.step(j, sign);
		final double hSigned	= h * sign;
		final double t			= intensities.grid.getNode(j);
		
		/*
		 * Indices of outward intensities
		 */
		
		final int n1 = sign > 0 ? nStart : nHalf;			//either first positive index or first negative (n/2)
		final int n2 = sign > 0 ? nHalf : intensities.n;	//either first negative index (n/2) or n
		
		int nH = n2 - n1;
		
		var error		= new double[nH];
		var curStage	= new double[nH];
		var q			= new double[nH][tableau.stages()];	
		
		double bDotQ;
		double sum;
		
		/*
		 * Indices of incoming intensities
		 */
		
		int n3 = intensities.n - n2; //either nHalf or nStart
		int n4 = intensities.n - n1; //either n or nHalf
		
		/*
		 * RK Explicit
		 */
		
		for (int m = 0; m < q.length; m++) { // stages
			
			/*
			 * Calculate interpolated intensities
			 */
			
			for(int l = n1; l < n2; l++) {	// unknown intensities (outward)
	
				sum = tableau.coefs.get(m, 0) * q[0][l - n1];
				for (int k = 1; k < m; k++)
					sum += tableau.coefs.get(m, k) * q[k][l - n1];
	
				curStage[l - n1] = intensities.I[j][l] + hSigned*sum;
				
			}
			
			/*
			 * Derivatives and associated errors
			 */
			
			for(int l = n1; l < n2; l++) {
				q[l - n1][m]   = rhs( l, j, t + hSigned * tableau.c.get(m), curStage, sign );
				error[l - n1] += (tableau.b.get(m) - tableau.bHat.get(m)) * q[l - n1][m] * hSigned;
			}
				
		}
			
		double[] Is = new double[n2 - n1];
		
		/*
		 * Value at next step
		 */
		
		for(int l = 0; l < nH; l++) {
			bDotQ = tableau.b.dot( new Vector( q[l] ) );
			Is[l] = intensities.I[j][l + n1] + bDotQ*hSigned; 
		}
			
		return new Vector[] { new Vector(Is), new Vector(error) };

	}

	public ButcherTableau getButcherTableau() {
		return tableau;
	}

	public void setButcherTableau(ButcherTableau coef) {
		this.tableau = coef;
	}

}