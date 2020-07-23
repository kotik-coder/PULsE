package pulse.problem.schemes.rte.dom;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

public class DiscreteIntensities extends PropertyHolder {

	private final static double DOUBLE_PI = 2.0 * Math.PI;

	protected double[][] I;

	protected StretchedGrid grid;
	protected OrdinateSet ordinates;

	private double emissivity;
	private double boundaryFluxFactor;
	private double qLeft, qRight;

	public DiscreteIntensities(ParticipatingMedium medium, int externalGridDensity) {
		ordinates = OrdinateSet.getDefaultInstance();

		this.grid = new StretchedGrid((double) medium.getOpticalThickness().getValue());
		setEmissivity(medium.getEmissivity());

		grid.setParent(this);

		medium.addListener(e -> {

			var p = e.getProperty();
			if (e.getProperty() instanceof NumericProperty) {
				var np = (NumericProperty) p;

				switch (np.getType()) {
				case EMISSIVITY:
					setEmissivity((double) np.getValue());
					break;
				case OPTICAL_THICKNESS:
					this.grid = new StretchedGrid((double) np.getValue());
					this.I = new double[grid.getDensity() + 1][ordinates.total];
					break;
				default:
					break;
				}

			}

		});

		this.reinitInternalArrays();
	}

	public void clear() {
		for (int i = 0, j = 0, N = grid.getDensity() + 1, n = ordinates.total; i < n; i++) {
                    for (j = 0; j < N; j++) {
                        I[j][i] = 0;
                    }
                }
		clearBoundaryFluxes();
	}

	public void clearBoundaryFluxes() {
		qLeft = 0.0;
		qRight = 0.0;
	}

	/*
	 * Interpolates intensities and their derivatives w.r.t. tau on EXTERNAL grid
	 * points of the heat problem solver based on the derivatives on INTERNAL grid
	 * points of DOM solver.
	 */

	public double[][][] interpolateOnExternalGrid(final int nE, final double[][] derivatives) {
		var Iext = new double[2][nE][ordinates.total];
		double t;

		final double hx = grid.getDimension() / (nE - 1.0);
		final int N = grid.getDensity() + 1;

		/*
		 * Loop through the external grid points
		 */
		outer: for (int i = 0, j = 0, k = 0; i < Iext[0].length; i++) {
			t = i * hx;

			// loops through nodes sorted in ascending order
			for (j = 0; j < N; j++) {

				/*
				 * if node is greater than t, then the associated function can be interpolated
				 * between points f_i and f_i-1, since t lies between nodes n_i and n_i-1
				 */
				if (grid.getNode(j) > t) { // nearest points on internal grid have been found -> j - 1 and j

					HermiteInterpolator.a = grid.getNode(j - 1);
					HermiteInterpolator.bMinusA = grid.stepLeft(j);

					/*
					 * Loops through ordinate set
					 */

					for (k = 0; k < ordinates.total; k++) {
						HermiteInterpolator.y0 = I[j - 1][k];
						HermiteInterpolator.y1 = I[j][k];
						HermiteInterpolator.d0 = derivatives[j - 1][k];
						HermiteInterpolator.d1 = derivatives[j][k];
						Iext[0][i][k] = HermiteInterpolator.interpolate(t); // intensity
						Iext[1][i][k] = HermiteInterpolator.derivative(t); // derivative
					}

					continue outer; // move to next point t

				}

			}

			for (k = 0; k < ordinates.total; k++) {
				Iext[0][i][k] = I[grid.getDensity()][k];
				Iext[1][i][k] = derivatives[grid.getDensity()][k];
			}

		}

		return Iext;
	}

	public double g(final int j) {
		double integral = 0;

		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		for (int i = nStart; i < nHalf; i++) {
                    integral += ordinates.w[i] * (I[j][i] + I[j][i + nHalf]);
                }

		if (ordinates.hasZeroNode())
			integral += ordinates.w[0] * I[j][0];

		return integral;

	}

	/**
	 * Calculates the net zeroth moment of intensity (i.e., the incident radiation)
	 * for the positive hemisphere).
	 * 
	 * @param j index on grid
	 * @return incident radiation (positive hemisphere)
	 */

	public double g(final int j, final int startInclusive, final int endExclusive) {
		double integral = 0;

		for (int i = startInclusive; i < endExclusive; i++) {
                    integral += ordinates.w[i] * I[j][i];
                }

		return integral;

	}

	public double q(final double[][] Iext, final int j) {
		double integral = 0;

		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		for (int i = nStart; i < nHalf; i++) {
                    integral += ordinates.w[i] * (Iext[j][i] - Iext[j][i + nHalf]) * ordinates.mu[i];
                }

		return integral;
	}

	public double q(final int j) {
		return q(I, j);
	}

	public double q(final int j, final int startInclusive, final int endExclusive) {
		double integral = 0;

		for (int i = startInclusive; i < endExclusive; i++) {
                    integral += ordinates.w[i] * I[j][i] * ordinates.mu[i];
                }

		return integral;
	}

	public double getEmissivity() {
		return emissivity;
	}

	public double[][] getIntensities() {
		return I;
	}

	public double getQLeft() {
		return qLeft;
	}

	public double getQRight() {
		return qRight;
	}

	/**
	 * Calculates the reflected intensity (positive angles, first half of indices)
	 * at the left boundary (tau = 0).
	 */

	public void left(final BlackbodySpectrum ef) {
		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		for (int i = nStart; i < nHalf; i++) {
                    // for positive streams
                    I[0][i] = ef.radianceAt(0.0) - boundaryFluxFactor * qLeft(ef);
                }

	}

	public double qLeft(final BlackbodySpectrum emissionFunction) {
		final int nHalf = ordinates.getFirstNegativeNode();
		return qLeft = emissivity * (Math.PI * emissionFunction.radianceAt(0.0) + DOUBLE_PI * q(0, nHalf, ordinates.total));
	}

	public double qRight(final BlackbodySpectrum emissionFunction) {
		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();
		return qRight = -emissivity
				* (Math.PI * emissionFunction.radianceAt(grid.getDimension()) - DOUBLE_PI * q(grid.getDensity(), nStart, nHalf));
	}

	public void reinitInternalArrays() {
		int N = grid.getDensity() + 1;
		I = new double[0][0];
		I = new double[N][ordinates.total];
	}

	/**
	 * Calculates the reflected intensity (negative angles, second half of indices)
	 * at the right boundary (tau = tau_0).
	 */

	public void right(final BlackbodySpectrum ef) {

		int N = grid.getDensity();

		final int nHalf = ordinates.getFirstNegativeNode();

		for (int i = nHalf; i < ordinates.total; i++) {
                    // for negative streams
                    I[N][i] = ef.radianceAt(grid.getDimension()) + boundaryFluxFactor * qRight(ef);
                }

	}

	private void setEmissivity(double emissivity) {
		this.emissivity = emissivity;
		boundaryFluxFactor = (1.0 - emissivity) / (emissivity * Math.PI);
	}

	public OrdinateSet getOrdinateSet() {
		return ordinates;
	}

	public void setOrdinateSet(OrdinateSet set) {
		this.ordinates = set;
		reinitInternalArrays();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		// intentionally left blank
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(OrdinateSet.getDefaultInstance());
		return list;
	}

	@Override
	public String getDescriptor() {
		return "Discretisation";
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("( ");
		sb.append("Quadrature: " + ordinates.getName() + " ; ");
		sb.append("Grid: " + grid.toString());
		return sb.toString();
	}

	public StretchedGrid getStretchedGrid() {
		return grid;
	}

	@Override
	public boolean ignoreSiblings() {
		return true;
	}

}