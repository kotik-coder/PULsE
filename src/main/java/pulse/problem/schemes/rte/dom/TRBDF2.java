package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Matrix;
import pulse.search.math.Vector;

public class TRBDF2 extends AdaptiveIntegrator {

	public final static double gamma = 2.0 - Math.sqrt(2.0);
	public final static double w = Math.sqrt(2.0) / 4.0;
	public final static double d = gamma / 2.0;

	private double k1[];
	private double k2[];
	private double k3[];

	private static double bHat2;
	private static double bHat3;
	private static double bHat1;

	private int nStart;
	private int nHalf;

	static {
		bHat1 = (1.0 - w) / 3.0;
		bHat2 = (3.0 * w + 1.0) / 3.0;
		bHat3 = d / 3.0;
	}

	public TRBDF2(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		nHalf = intensities.quadratureSet.getFirstNegativeNode();
		nStart = intensities.quadratureSet.getFirstPositiveNode();
	}

	@Override
	public void integrate() {
		int N = intensities.grid.getDensity();

		Vector[] v;

		outer: for (double erSq = 1.0; erSq > adaptiveErSq; N = intensities.grid.getDensity()) {

			erSq = 0;
			f = new double[N + 1][intensities.n];

			treatZeroIndex();

			/*
			 * First set of ODE's. Initial condition corresponds to I(0) /t ----> tau0 The
			 * streams propagate in the positive hemisphere
			 */

			//initial condition for first set
			intensities.left(emissionFunction); // initial value for tau = 0
			
			for (int j = 0, i; j < N && erSq < adaptiveErSq; j++) {
				v = step(j, 1.0);
				erSq = v[1].lengthSq();
				System.arraycopy(v[0].getData(), 0, intensities.I[j + 1], nStart, nHalf - nStart); 
			}
			
			//initial condition for second set
			intensities.right(emissionFunction); // initial value for tau = tau_0

			/*
			 * Second set of ODE. Initial condition corresponds to I(tau0) /0 <---- t The
			 * streams propagate in the negative hemisphere
			 */
			
			for (int j = N, i = 0; j > 0 && erSq < adaptiveErSq; j--) {
				v = step(j, -1.0);
				erSq = v[1].lengthSq();
				System.arraycopy(v[0].getData(), 0, intensities.I[j - 1], nHalf, nHalf - nStart); 
			}
			
			if (erSq < adaptiveErSq)
				break outer;
			else {
				reduceStepSize();
				f = new double[0][0];
				HermiteInterpolator.clear();
			}

		}

	}

	public Vector[] step(int j, double sign) {

		double[] bVector	= new double[nHalf - nStart];
		double[] est		= new double[bVector.length];
		double[][] aMatrix	= new double[bVector.length][bVector.length];

		Matrix invA;
		Vector i2, i3;

		k1 = new double[bVector.length];
		k2 = new double[k1.length];
		k3 = new double[k2.length];

		double factor;

		double h		= sign * intensities.grid.step(j, sign);
		HermiteInterpolator.len = h;
		
		int increment	= (int) (1 * sign);
		double t		= intensities.grid.getNode(j); // TODO interpolate
		HermiteInterpolator.a = t;
		
		double halfAlbedo = getAlbedo() * 0.5;

		/*
		 * Indices of outward intensities
		 */
		
		int n1 = sign > 0 ? nStart : nHalf;			//either first positive index or first negative (n/2)
		int n2 = sign > 0 ? nHalf : intensities.n;	//either first negative index (n/2) or n
		
		/*
		 * Indices of incoming intensities
		 */
		
		int n3 = intensities.n - n2; //either nHalf or nStart
		int n4 = intensities.n - n1; //either n or nHalf

		/*
		 * Trapezoidal scheme
		 */
		
		final double hd		= h*d;
		final double tgamma = t + gamma * h;
		
		for (int i = 0; i < aMatrix.length; i++) {

			HermiteInterpolator.y0 = intensities.I[j][i + n3];
			HermiteInterpolator.y1 = intensities.I[j + increment][i + n3];
			HermiteInterpolator.d0 = f[j][i + n3];
			HermiteInterpolator.d1 = f[j + increment][i + n3];
			
			k1[i] = super.rhs(i + n1, j, t, intensities.I[j][i + n1] ); //first-stage right-hand side: f( t, I[i+n1] )
			f[j][i + n1] = k1[i];

			// b for linear system 
			bVector[i] = intensities.I[j][i + n1] + hd*k1[i] + hd/intensities.mu[i + n1] * 	 
						   (super.sourceEmission(tgamma)  
							+ halfAlbedo * ipf.integratePartial(i + n1, HermiteInterpolator.interpolate(tgamma), n3, n4) );  // omega_0/2 * sum (w[k] * Ph(i + n1, k) * I[k][j] ), sum over incoming intensities

			factor = -hd * halfAlbedo / intensities.mu[i + n1];

			// all elements
			for (int k = 0; k < aMatrix[0].length; k++)
				aMatrix[i][k] = factor * intensities.w[k + n1] * ipf.function(i + n1, k + n1);	//ipf -> sum over outward intensities

			// additionally for the diagonal elements
			aMatrix[i][i] += 1.0 + hd / intensities.mu[i + n1];

		}

		invA = (new Matrix(aMatrix)).inverse();		//same matrix for 2nd and 3rd stage plus for Est

		i2 = invA.multiply(new Vector(bVector));	//second stage (trapezoidal)

		/*
		 * + BDF2
		 */

		final double w_d = w/d;
		final double _1w_d = (1.0 - w_d);
		final double th = t + h;
		
		for (int i = 0; i < aMatrix.length; i++) {

			bVector[i] = intensities.I[j][i + n1] * _1w_d + w_d * i2.get(i)
					+ hd / intensities.mu[i + n1] * (super.sourceEmission(th) 
							+ halfAlbedo * ipf.integratePartial(i + n1, j + increment, n3, n4) ); //sum over incoming intensities
			k2[i] = (i2.get(i) - intensities.I[j][i + n1]) / hd - k1[i];
		
		}

		i3 = invA.multiply(new Vector(bVector));

		for (int i = 0; i < aMatrix.length; i++) {
			k3[i] = (i3.get(i) - intensities.I[j][i + n1] - w / d * (i2.get(i) - intensities.I[j][i + n1])) / hd;
			est[i] += ((w - bHat1) * k1[i] + (w - bHat2) * k2[i] + (d - bHat3) * k3[i]) * h;
		}

		return new Vector[] { i3, invA.multiply(new Vector(est)) };

	}

}