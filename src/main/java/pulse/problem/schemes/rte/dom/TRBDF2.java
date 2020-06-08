package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;
import pulse.search.math.Matrix;
import pulse.search.math.Vector;

public class TRBDF2 extends AdaptiveIntegrator {

	/*
	 * Coefficients of the Butcher tableau as originally defined in
	 * M.E. Hosea, L.E Shampine/Applied Numerical Mathematics 20 (1996) 21-37
	 */
	
	private final static double gamma = 2.0 - Math.sqrt(2.0);
	private final static double w = Math.sqrt(2.0) / 4.0;
	private final static double d = gamma / 2.0;

	/*
	 * Coefficients for the error estimator. These are taken from:
	 * Christopher A. Kennedy, Mark H. Carpenter. Diagonally Implicit Runge-Kutta Methods
	 * for Ordinary Differential Equations. A Review. NASA/TM–2016–219173, p. 72
	 */
	
	private static double bHat2;
	private static double bHat3;
	private static double bHat1;
	
	static {
		double g	= gamma / 2.0;
		bHat2		= g*(-2.0 + 7.0*g - 5.0*g*g + 4.0*g*g*g)/(2.0*(2.0*g - 1.0));
		bHat3		= (-2.0*g*g)*(1.0 - g + g*g)/(2.0*g - 1.0);
		bHat1		= 1.0 - bHat2 - bHat3;
	}
	
	/*
	 * ===
	 */
	
	private double k[][];

	private int nPositiveStart;
	private int nNegativeStart;

	public TRBDF2(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		nNegativeStart	= intensities.quadratureSet.getFirstNegativeNode();
		nPositiveStart	= intensities.quadratureSet.getFirstPositiveNode();
	}

	/**
	 * Performs a TRBDF2 step.
	 */
	
	public Vector[] step(final int j, final double sign) {

		double[] bVector	= new double[nNegativeStart - nPositiveStart];	//right-hand side of linear set A * x = B
		double[] est		= new double[bVector.length];	//error estimator
		double[][] aMatrix	= new double[bVector.length][bVector.length];	//matrix of linear set A * x = B

		Matrix invA;	//inverse matrix
		
		Vector i2;		//second stage (trapezoidal)
		Vector i3;		//third stage (backward-difference second order)

		k = new double[3][bVector.length];

		double matrixPrefactor;

		double h		= sign * intensities.grid.step(j, sign);
		HermiteInterpolator.bMinusA = h;	// <---- for Hermite interpolation
		
		int increment	= (int) (1 * sign);
		double t		= intensities.grid.getNode(j); // TODO interpolate
		HermiteInterpolator.a = t;			// <---- for Hermite interpolation
		
		double halfAlbedo = getAlbedo() * 0.5;

		/*
		 * Indices of OUTWARD intensities (n1 <= i < n2)
		 */
		
		int n1 = sign > 0 ? nPositiveStart : nNegativeStart;	//either first positive index or first negative
		int n2 = sign > 0 ? nNegativeStart : intensities.n;		//either first negative index or n
		
		/*
		 * Indices of INWARD intensities (n3 <= i < n4)
		 */
		
		int n3 = intensities.n - n2;		//either first negative index or 0 (for INWARD intensities)
		int n4 = intensities.n - n1;		//either n or first negative index (for INWARD intensities)
		int n5 = nNegativeStart - n3;		//either 0 or first negative index

		/*
		 * =============================
		 * 1st and 2nd stages begin here
		 * =============================
		 */
		
		final double hd			= h*d;
		final double tPlusGamma = t + gamma * h;
		
		/*
		 * Interpolate INWARD intensities at t + gamma*h (second stage)
		 */
		
		double[] inward = new double[nNegativeStart - nPositiveStart];
		
		for(int i = 0; i < inward.length; i++) {
			HermiteInterpolator.y0 = intensities.I[j][i + n3];
			HermiteInterpolator.y1 = intensities.I[j + increment][i + n3];
			HermiteInterpolator.d0 = f[j][i + n3];
			HermiteInterpolator.d1 = f[j + increment][i + n3];
			inward[i] = HermiteInterpolator.interpolate(tPlusGamma);
		}
		
		/*
		 * Trapezoidal step
		 */
		
		for (int i = 0; i < aMatrix.length; i++) {
			
			k[0][i] = super.derivative(i + n1, j, t, intensities.I[j][i + n1] ); //first-stage right-hand side: f( t, I[i+n1] )
			f[j][i + n1] = k[0][i];

			bVector[i] = intensities.I[j][i + n1] + hd*( k[0][i] + partial(i + n1, tPlusGamma, inward, n3, n4) ); //only INWARD intensities

			matrixPrefactor = -hd * halfAlbedo / intensities.mu[i + n1];

			// all elements
			for (int k = 0; k < aMatrix[0].length; k++)
				aMatrix[i][k] = matrixPrefactor * intensities.w[k + n5] * pf.function(i + n1, k + n5);	//only OUTWARD (and zero) intensities

			// additionally for the diagonal elements
			aMatrix[i][i] += 1.0 + hd / intensities.mu[i + n1];

		}

		invA = (new Matrix(aMatrix)).inverse();		//this matrix is re-used for subsequent stages
		i2 = invA.multiply(new Vector(bVector));	//intensity vector at 2nd stage

		/* ==================
		 * Third stage (BDF2)
		 * ==================
		 */

		final double w_d = w/d;
		final double _1w_d = (1.0 - w_d);
		final double th = t + h;
		
		for (int i = 0; i < aMatrix.length; i++) {

			bVector[i] = intensities.I[j][i + n1] * _1w_d + w_d * i2.get(i)	
					   + hd * partial(i + n1, j + increment, th, n3, n4); //only INWARD intensities at node j + 1 (i.e. no interpolation)
			k[1][i] = (i2.get(i) - intensities.I[j][i + n1]) / hd - k[0][i];
		
		}

		i3 = invA.multiply(new Vector(bVector));

		for (int i = 0; i < aMatrix.length; i++) {
			k[2][i] = (i3.get(i) - intensities.I[j][i + n1] - w / d * (i2.get(i) - intensities.I[j][i + n1])) / hd;
			est[i] += ((w - bHat1) * k[0][i] + (w - bHat2) * k[1][i] + (d - bHat3) * k[2][i]) * h;
		}

		return new Vector[] { i3, invA.multiply(new Vector(est)) };

	}

}