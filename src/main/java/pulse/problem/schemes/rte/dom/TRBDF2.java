package pulse.problem.schemes.rte.dom;

import pulse.math.linear.Matrices;
import pulse.math.linear.SquareMatrix;
import pulse.math.linear.Vector;
import pulse.problem.schemes.rte.RTECalculationStatus;

/**
 * TRBDF2 (Trapezoidal Backward Differencing Second Order) Scheme for the solution of one-dimensional radiative transfer problems.
 * 
 * @author Artem Lunev, Vadim Zborovskii
 *
 */

public class TRBDF2 extends AdaptiveIntegrator {

	/*
	 * Coefficients of the Butcher tableau as originally defined in M.E. Hosea, L.E
	 * Shampine/Applied Numerical Mathematics 20 (1996) 21-37
	 */

	private final static double gamma = 2.0 - Math.sqrt(2.0);
	private final static double w = Math.sqrt(2.0) / 4.0;
	private final static double d = gamma / 2.0;

	/*
	 * Uncomment this for coefficients for the error estimator from: Christopher A.
	 * Kennedy, Mark H. Carpenter. Diagonally Implicit Runge-Kutta Methods for
	 * Ordinary Differential Equations. A Review. NASA/TM–2016–219173, p. 72
	 * 
	 * double g = gamma / 2.0; bHat[1] = g * (-2.0 + 7.0 * g - 5.0 * g * g + 4.0 * g
	 * * g * g) / (2.0 * (2.0 * g - 1.0)); bHat[2] = (-2.0 * g * g) * (1.0 - g + g *
	 * g) / (2.0 * g - 1.0); bHat[0] = 1.0 - bHat[1] - bHat[2];
	 *
	 * 
	 */

	private static double[] bHat = new double[3];

	/*
	 * These are the original error estimator coefficients.
	 */

	static {
		bHat[0] = (1.0 - w) / 3.0;
		bHat[1] = (3.0 * w + 1.0) / 3.0;
		bHat[2] = d / 3.0;
	}

	private final static double[] bbHat = new double[] { w - bHat[0], w - bHat[1], d - bHat[2] };

	private double k[][];

	private double[] inward;
	private double[] bVector; // right-hand side of linear set A * x = B
	private double[] est; // error estimator
	private double[][] aMatrix; // matrix of linear set A * x = B

	private SquareMatrix invA; // inverse matrix
	private Vector i2; // second stage (trapezoidal)
	private Vector i3; // third stage (backward-difference second order)

	/*
	 * Constants for third-stage calculation
	 */

	private double w_d = w / d;
	private double _1w_d = (1.0 - w_d);

	public TRBDF2(Discretisation intensities) {
		super(intensities);
	}
	
	@Override
	public RTECalculationStatus integrate() {
		final int nH = getDiscretisation().getOrdinates().getHalfLength();

		bVector = new double[nH];
		est = new double[nH];
		aMatrix = new double[nH][nH];
		inward = new double[nH];

		k = new double[3][nH];
		return super.integrate();
	}
	
	/**
	 * Generates a non-uniform (stretched at boundaries) grid using the 
	 * argument as the density.
	 * @param nNew new grid density
	 */

	@Override
	public void generateGrid(int nNew) {
		getDiscretisation().getGrid().generate(nNew);
	}

	/**
	 * Performs a TRBDF2 step.
	 */

	@Override
	public Vector[] step(final int j, final double sign) {
		var intensities = getDiscretisation();
		var quantities = intensities.getQuantities();
		var hermite = getHermiteInterpolator();
		
		final double h = sign * intensities.getGrid().step(j, sign);
		hermite.bMinusA = h; // <---- for Hermite interpolation

		final int total = getDiscretisation().getOrdinates().getTotalNodes();
		final int increment = (int) (1 * sign);
		final double t = intensities.getGrid().getNode(j);
		hermite.a = t; // <---- for Hermite interpolation

		/*
		 * Indices of OUTWARD intensities (n1 <= i < n2)
		 */

		final int nPositiveStart = intensities.getOrdinates().getFirstPositiveNode();
		final int nNegativeStart = intensities.getOrdinates().getFirstNegativeNode();
		final int halfLength = nNegativeStart - nPositiveStart;
		final int n1 = sign > 0 ? nPositiveStart : nNegativeStart; // either first positive index or first negative
		final int n2 = sign > 0 ? nNegativeStart : total; // either first negative index or n

		/*
		 * Indices of INWARD intensities (n3 <= i < n4)
		 */

		final int n3 = total - n2; // either first negative index or 0 (for INWARD intensities)
		final int n4 = total - n1; // either n or first negative index (for INWARD intensities)
		final int n5 = nNegativeStart - n3; // either 0 or first negative index

		/*
		 * Try to use FSAL
		 */

		if (!isFirstRun()) { // if this is not the first step

			for (int l = n1; l < n2; l++) {
				k[0][l - n1] = quantities.getQLast(l - n1);
			}

		} else {

			for (int l = n1; l < n2; l++) {
				k[0][l - n1] = super.derivative(l, j, t, quantities.getIntensity(j, l)); // first-stage right-hand
																							// side: f( t, In)
				// )
			} // )

			setFirstRun(false);

		}

		/*
		 * ============================= 1st and 2nd stages begin here
		 * =============================
		 */

		final double hd = h * d;
		final double tPlusGamma = t + gamma * h;

		/*
		 * Interpolate INWARD intensities at t + gamma*h (second stage)
		 */

		for (int i = 0; i < inward.length; i++) {
			hermite.y0 = quantities.getIntensity(j, i + n3);
			hermite.y1 = quantities.getIntensity(j + increment, i + n3);
			hermite.d0 = quantities.getDerivative(j, i + n3);
			hermite.d1 = quantities.getDerivative(j + increment, i + n3);
			inward[i] = hermite.interpolate(tPlusGamma);
		}

		/*
		 * Trapezoidal step
		 */

		final double prefactorNumerator = -hd * getPhaseFunction().getHalfAlbedo();
		
		double matrixPrefactor;
		final var ordinates = intensities.getOrdinates();

		for (int i = 0; i < halfLength; i++) {

			quantities.setDerivative(j, i + n1, k[0][i]); // store derivatives for Hermite interpolation

			bVector[i] = quantities.getIntensity(j, i + n1)
					+ hd * (k[0][i] + partial(i + n1, tPlusGamma, inward, n3, n4)); // only
			// INWARD
			// intensities

			matrixPrefactor = prefactorNumerator / ordinates.getNode(i + n1);

			// all elements
			for (int k = 0; k < aMatrix[0].length; k++) {
				aMatrix[i][k] = matrixPrefactor * ordinates.getWeight(k + n5)
						* getPhaseFunction().function(i + n1, k + n5); // only
				// OUTWARD
				// (and zero)
				// intensities
			}

			// additionally for the diagonal elements
			aMatrix[i][i] += 1.0 + hd / ordinates.getNode(i + n1);

		}

		invA = (Matrices.createSquareMatrix(aMatrix)).inverse(); // this matrix is re-used for subsequent stages
		i2 = invA.multiply(new Vector(bVector)); // intensity vector at 2nd stage

		/*
		 * ================== Third stage (BDF2) ==================
		 */

		final double th = t + h;

		for (int i = 0; i < aMatrix.length; i++) {

			bVector[i] = quantities.getIntensity(j, i + n1) * _1w_d + w_d * i2.get(i)
					+ hd * partial(i + n1, j + increment, th, n3, n4); // only INWARD intensities at node j + 1 (i.e. no
																		// interpolation)
			k[1][i] = (i2.get(i) - quantities.getIntensity(j, i + n1)) / hd - k[0][i];

		}

		i3 = invA.multiply(new Vector(bVector));

		for (int i = 0; i < aMatrix.length; i++) {
			k[2][i] = (i3.get(i) - quantities.getIntensity(j, i + n1)
					- w_d * (i2.get(i) - quantities.getIntensity(j, i + n1))) / hd;
			quantities.setQLast(i, k[2][i]);
			est[i] = (bbHat[0] * k[0][i] + bbHat[1] * k[1][i] + bbHat[2] * k[2][i]) * h;
		}

		return new Vector[] { i3, invA.multiply(new Vector(est)) };

	}

}