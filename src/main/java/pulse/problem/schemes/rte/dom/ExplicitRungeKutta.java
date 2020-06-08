package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Vector;

public class ExplicitRungeKutta extends AdaptiveIntegrator {

	private ButcherTableau tableau;
	private int nStart;
	private int nHalf;
	
	public ExplicitRungeKutta(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		tableau	= ButcherTableau.DEFAULT_TABLEAU;
		nHalf	= intensities.quadratureSet.getFirstNegativeNode();
		nStart	= intensities.quadratureSet.getFirstPositiveNode();
	}

	@Override
	public Vector[] step(final int j, final double sign) {

		final double h			= intensities.grid.step(j, sign);
		final double hSigned	= h * sign;
		final double t			= intensities.grid.getNode(j);
		
		HermiteInterpolator.a	= t;
		HermiteInterpolator.bMinusA = hSigned;
		
		/*
		 * Indices of outward (n1 to n2) and inward (> n3) intensities
		 */
		
		final int n1 = sign > 0 ? nStart : nHalf;			//either first positive index or first negative (n/2)
		final int n2 = sign > 0 ? nHalf : intensities.n;	//either first negative index (n/2) or n
		final int n3 = intensities.n - n2; 					//either nHalf or nStart
		
		int nH = n2 - n1;
		
		var error		= new double[nH];
		var iOutward	= new double[nH];
		var q			= new double[nH][tableau.stages()];	
		
		double bDotQ;
		double sum;
		
		double[] iIncoming = new double[nH];
		
		int increment	= (int) (1 * sign);
		
		/*
		 * RK Explicit
		 */

		/*
		 * First stage
		 */
		
		for(int l = n1; l < n2; l++) {
			q[l - n1][0]	= derivative( l, j, t, intensities.I[j][l] );
			f[j][l]			= q[l - n1][0]; //store derivative for incoming intensities
			error[l - n1]	+= (tableau.b.get(0) - tableau.bHat.get(0)) * q[l - n1][0] * hSigned;
		}
		
		/*
		 * Other stages
		 */
		
		for (int m = 1; m < q.length; m++) { // stages
			
			/*
			 * Calculate interpolated (OUTWARD and INCOMING) intensities
			 */
			
			double tm = t + hSigned * tableau.c.get(m); //interpolation point
			
			for(int l = n1; l < n2; l++) {	// unknown intensities (outward)
	
				/*
				 * OUTWARD
				 */
			
				sum = tableau.coefs.get(m, 0) * q[0][l - n1];
				for (int k = 1; k < m; k++)
					sum += tableau.coefs.get(m, k) * q[k][l - n1];
	
				iOutward[l - n1] = intensities.I[j][l] + hSigned*sum;
				
				/*
				 * INCOMING
				 */
				
				HermiteInterpolator.y0 = intensities.I[j][l + n3];
				HermiteInterpolator.y1 = intensities.I[j + increment][l + n3];
				HermiteInterpolator.d0 = f[j][l + n3];
				HermiteInterpolator.d1 = f[j + increment][l + n3];
				
				iIncoming[l - n1] = HermiteInterpolator.interpolate( tm );
				
			}
			
			/*
			 * Derivatives and associated errors
			 */
			
			for(int l = n1; l < n2; l++) {
				q[l - n1][m]   = derivative( l, tm, iOutward, iIncoming, n1, n2 );
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