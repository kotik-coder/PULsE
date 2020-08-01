package pulse.problem.schemes.rte.dom;

import static java.lang.Math.PI;

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

	private double[][] I;

	private StretchedGrid grid;
	private OrdinateSet ordinates;

	private double emissivity;
	private double boundaryFluxFactor;
	private double qLeft, qRight;

	public DiscreteIntensities(ParticipatingMedium problem) {
		ordinates = OrdinateSet.getDefaultInstance();

		this.grid = new StretchedGrid((double) problem.getOpticalThickness().getValue());
		setEmissivity(problem.getEmissivity());

		grid.setParent(this);
		this.reinitInternalArrays();
	}

	public void clear() {
		final int n = grid.getDensity() + 1;
		final int m = ordinates.getTotalNodes();
		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				I[j][i] = 0;
			}
		}
		clearBoundaryFluxes();
	}

	public void clearBoundaryFluxes() {
		qLeft = 0.0;
		qRight = 0.0;
	}

	public double inidentRadation(final int j) {
		double integral = 0;

		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		//uses symmetry
		for (int i = nStart; i < nHalf; i++) {
			integral += ordinates.getWeight(i) * (I[j][i] + I[j][i + nHalf]);
		}

		if (ordinates.hasZeroNode())
			integral += ordinates.getWeight(0) * I[j][0];

		return integral;
	}

	/**
	 * Calculates the net zeroth moment of intensity (i.e., the incident radiation)
	 * for the positive hemisphere).
	 * 
	 * @param j index on grid
	 * @return incident radiation (positive hemisphere)
	 */

	public double incidentRadiation(final int j, final int startInclusive, final int endExclusive) {
		double integral = 0;

		for (int i = startInclusive; i < endExclusive; i++) {
			integral += ordinates.getWeight(i) * I[j][i];
		}

		return integral;

	}

	public double flux(final double[][] iExt, final int j) {
		double integral = 0;

		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		// uses symmetry
		for (int i = nStart; i < nHalf; i++) {
			integral += ordinates.getWeight(i) * (iExt[j][i] - iExt[j][i + nHalf]) * ordinates.getNode(i);
		}

		return integral;
	}

	public double flux(final int j) {
		return flux(I, j);
	}

	public double flux(final int j, final int startInclusive, final int endExclusive) {
		double integral = 0;

		for (int i = startInclusive; i < endExclusive; i++) {
			integral += ordinates.getWeight(i) * I[j][i] * ordinates.getNode(i);
		}

		return integral;
	}

	public double getEmissivity() {
		return emissivity;
	}

	public double[][] getIntensities() {
		return I;
	}

	public double getFluxLeft() {
		return qLeft;
	}

	public double getFluxRight() {
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
			I[0][i] = ef.radianceAt(0.0) - boundaryFluxFactor * fluxLeft(ef);
		}

	}

	public double fluxLeft(final BlackbodySpectrum emissionFunction) {
		final int nHalf = ordinates.getFirstNegativeNode();
		return qLeft = emissivity
				* (PI * emissionFunction.radianceAt(0.0) + DOUBLE_PI * flux(0, nHalf, ordinates.getTotalNodes()));
	}

	public double fluxRight(final BlackbodySpectrum emissionFunction) {
		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();
		return qRight = -emissivity * (PI * emissionFunction.radianceAt(grid.getDimension())
				- DOUBLE_PI * flux(grid.getDensity(), nStart, nHalf));
	}

	public void reinitInternalArrays() {
		int N = grid.getDensity() + 1;
		I = new double[0][0];
		I = new double[N][ordinates.getTotalNodes()];
	}

	/**
	 * Calculates the reflected intensity (negative angles, second half of indices)
	 * at the right boundary (tau = tau_0).
	 */

	public void right(final BlackbodySpectrum ef) {

		final int N = grid.getDensity();
		final int nHalf = ordinates.getFirstNegativeNode();

		for (int i = nHalf; i < ordinates.getTotalNodes(); i++) {
			// for negative streams
			I[N][i] = ef.radianceAt(grid.getDimension()) + boundaryFluxFactor * fluxRight(ef);
		}

	}

	public void setEmissivity(double emissivity) {
		this.emissivity = emissivity;
		boundaryFluxFactor = (1.0 - emissivity) / (emissivity * Math.PI);
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

	public StretchedGrid getGrid() {
		return grid;
	}

	public void setGrid(StretchedGrid grid) {
		this.grid = grid;
		reinitInternalArrays();
	}

	public OrdinateSet getOrdinates() {
		return ordinates;
	}

	public void setOrdinates(OrdinateSet ordinates) {
		this.ordinates = ordinates;
	}

	public double getIntensity(int i, int j) {
		return I[i][j];
	}

	public void setIntensity(int i, int j, double value) {
		I[i][j] = value;
	}

}