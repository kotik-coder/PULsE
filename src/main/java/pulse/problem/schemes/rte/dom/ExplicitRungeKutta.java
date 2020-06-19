package pulse.problem.schemes.rte.dom;

import java.util.List;

import pulse.math.Vector;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.properties.Property;

/**
 * Explicit Runge-Kutta.
 * 
 * @author Artem Lunev, Vadim Zborovskii
 *
 */

public class ExplicitRungeKutta extends AdaptiveIntegrator {

	private ButcherTableau tableau;

	public ExplicitRungeKutta(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
		tableau = ButcherTableau.getDefaultInstance();
	}

	@Override
	public Vector[] step(final int j, final double sign) {

		final double h = intensities.grid.step(j, sign);
		final double hSigned = h * sign;
		final double t = intensities.grid.getNode(j);

		HermiteInterpolator.a = t;
		HermiteInterpolator.bMinusA = hSigned;

		/*
		 * Indices of outward (n1 to n2) and inward (> n3) intensities
		 */

		final int n1 = sign > 0 ? nPositiveStart : nNegativeStart; // either first positive index (e.g. 0) or first
																	// negative (n/2)
		final int n2 = sign > 0 ? nNegativeStart : intensities.ordinates.total; // either first negative index (n/2) or
																				// n
		final int n3 = intensities.ordinates.total - n2; // either nNegativeStart or 0
		final int nH = n2 - n1;

		var error = new double[nH];
		var iOutward = new double[nH];
		var iInward = new double[nH];

		int stages = tableau.numberOfStages();

		var q = new double[nH][stages]; // first index - cosine node, second index - stage

		double bDotQ;
		double sum;

		int increment = (int) (1 * sign);

		/*
		 * RK Explicit (Embedded)
		 */

		/*
		 * First stage
		 */

		if (tableau.isFSAL() && !firstRun) { // if FSAL

			for (int l = n1; l < n2; l++)
				q[l - n1][0] = qLast[l - n1]; // assume first stage is the last stage of last step

		} else { // if not FSAL or on first run

			for (int l = n1; l < n2; l++)
				q[l - n1][0] = derivative(l, j, t, intensities.I[j][l]);

			firstRun = false;

		}

		// in any case

		for (int l = n1; l < n2; l++) {
			f[j][l] = q[l - n1][0]; // store derivative for inward intensities
			error[l - n1] = (tableau.b.get(0) - tableau.bHat.get(0)) * q[l - n1][0] * hSigned;
		}

		/*
		 * Next stages
		 */

		for (int m = 1; m < stages; m++) { // <------- STAGES (1...s)

			/*
			 * Calculate interpolated (OUTWARD and INWARD) intensities at each stage from m
			 * = 1 onwards
			 */

			double tm = t + hSigned * tableau.c.get(m); // interpolation point for stage m

			for (int l = n1; l < n2; l++) { // find unknown intensities (sum over the outward intensities)

				/*
				 * OUTWARD
				 */

				sum = tableau.coefs.get(m, 0) * q[l - n1][0];
				for (int k = 1; k < m; k++)
					sum += tableau.coefs.get(m, k) * q[l - n1][k];

				iOutward[l - n1] = intensities.I[j][l] + hSigned * sum; // outward intensities are simply found from the
																		// RK explicit expressions

				/*
				 * INWARD
				 */

				HermiteInterpolator.y0 = intensities.I[j][l + n3];
				HermiteInterpolator.y1 = intensities.I[j + increment][l + n3];
				HermiteInterpolator.d0 = f[j][l + n3];
				HermiteInterpolator.d1 = f[j + increment][l + n3];

				iInward[l - n1] = HermiteInterpolator.interpolate(tm); // inward intensities are interpolated with
																		// Hermite polynomials

			}

			/*
			 * Derivatives and associated errors at stage m
			 */

			for (int l = n1; l < n2; l++) {
				q[l - n1][m] = derivative(l, tm, iOutward, iInward, n1, n2);
				qLast[l - n1] = q[l - n1][m];
				error[l - n1] += (tableau.b.get(m) - tableau.bHat.get(m)) * q[l - n1][m] * hSigned;
			}

		}

		double[] Is = new double[nH];

		/*
		 * Value at next step
		 */

		for (int l = 0; l < nH; l++) {
			bDotQ = tableau.b.dot(new Vector(q[l]));
			Is[l] = intensities.I[j][l + n1] + bDotQ * hSigned;
		}

		return new Vector[] { new Vector(Is), new Vector(error) };

	}

	public ButcherTableau getButcherTableau() {
		return tableau;
	}

	public void setButcherTableau(ButcherTableau coef) {
		this.tableau = coef;
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(ButcherTableau.getDefaultInstance());
		return list;
	}

	@Override
	public String toString() {
		return super.toString() + " ; " + tableau;
	}

}