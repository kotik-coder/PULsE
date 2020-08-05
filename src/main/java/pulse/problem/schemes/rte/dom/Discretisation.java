package pulse.problem.schemes.rte.dom;

import static java.lang.Math.PI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.problem.schemes.rte.BlackbodySpectrum;
import pulse.problem.statements.ParticipatingMedium;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

/**
 * A class specifying the discretisation (both spatial and angular) scheme used
 * to solve the radiative transfer equation with the discrete ordinates method.
 * Has references to the grid and ordinate sets and the discrete quantities appearing
 * in the calculations. Introduces methods to calculate the moments and intensities
 * based on the discrete ordinates method.
 *
 */

public class Discretisation extends PropertyHolder {

	private DiscreteQuantities quantities;
	private StretchedGrid grid;
	private OrdinateSet ordinates;

	private double emissivity;
	private double boundaryFluxFactor;
	
	/**
	 * Constructs a {@code DiscreteIntensities} with the default {@code OrdinateSet} and a new 
	 * uniform grid.
	 * @param problem the problem statement
	 */

	public Discretisation(ParticipatingMedium problem) {
		ordinates = OrdinateSet.defaultSet();
		setGrid( new StretchedGrid((double) problem.getOpticalThickness().getValue()) );
		quantities = new DiscreteQuantities(grid.getDensity(), ordinates.getTotalNodes());
		setEmissivity(problem.getEmissivity());
	}
		
	/**
	 * Calculates the incident radiation <math>&#8721;<sub>i</sub><i>w<sub>i</sub> I<sub>ij</sub></i></math>,
	 * which is the zeroth moment of the intensities. The calculation uses the symmetry of the quadrature
	 * weights for positive and negativ nodes.
	 * @param j spatial index
	 * @return the incident radiation at {@code j}
	 */
	
	public double incidentRadation(final int j) {
		double integral = 0;

		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		// uses symmetry
		for (int i = nStart; i < nHalf; i++) {
			integral += ordinates.getWeight(i) * ( quantities.getIntensity(j, i) + quantities.getIntensity(j,i + nHalf) );
		}

		if (ordinates.hasZeroNode())
			integral += ordinates.getWeight(0) * quantities.getIntensity(j, 0);

		return integral;
	}

	/**
	 * Calculates the incident radiation <math>&#8721;<sub>i</sub><i>w<sub>i</sub> I<sub>ij</sub></i></math>,
	 * by performing simple summation for node points between {@code startInclusive} and {@code endExclusive}.
	 * @param j spatial index
	 * @param startInclusive lower bound for summation
	 * @param endExclusive upper bound (exclusive) for summation
	 * @return the partial incident radiation at {@code j} 
	 * @see pulse.problem.schemes.rte.dom.LinearAnisotropicPF
	 */

	public double incidentRadiation(final int j, final int startInclusive, final int endExclusive) {
		double integral = 0;

		for (int i = startInclusive; i < endExclusive; i++) {
			integral += ordinates.getWeight(i) * quantities.getIntensity(j, i);
		}

		return integral;

	}
	
	/**
	 * Calculates the first moment <math>&#8721;<sub>i</sub><i>w<sub>i</sub>&mu;<sub>i</sub>ext<sub>ij</sub></i></math>,
	 * which can be applied e.g. for flux or flux derivative calculation. The calculation uses the symmetry of the quadrature
	 * weights for positive and negativ nodes.
	 * @param j spatial index
	 * @return the first moment at {@code j}
	 */

	public double firstMoment(final double[][] ext, final int j) {
		double integral = 0;

		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		// uses symmetry
		for (int i = nStart; i < nHalf; i++) {
			integral += ordinates.getWeight(i) * (ext[j][i] - ext[j][i + nHalf]) * ordinates.getNode(i);
		}

		return integral;
	}
	
	/**
	 * Calculates the net flux at {@code j}.
	 * @param j the spatial coordinate
	 * @return the flux
	 * @see firstMoment(double[][],int)
	 */

	public double flux(final int j) {
		return firstMoment(quantities.getIntensities(), j);
	}
	
	/**
	 * Calculates the partial flux by performing a simple summation bounded by the arguments.
	 * @param j the spatial index
	 * @param startInclusive node index lower bound
	 * @param endExclusive node index upper bound (exclusive)
	 * @return the partial flux
	 */

	public double flux(final int j, final int startInclusive, final int endExclusive) {
		double integral = 0;

		for (int i = startInclusive; i < endExclusive; i++) {
			integral += ordinates.getWeight(i) * quantities.getIntensity(j, i) * ordinates.getNode(i);
		}

		return integral;
	}
	
	/**
	 * Calculates the flux at the left boundary using an alternative formula.
	 * @param emissionFunction the emission function
	 * @return the net flux at the left boundary
	 */
	
	public double fluxLeft(final BlackbodySpectrum emissionFunction) {
		final int nHalf = ordinates.getFirstNegativeNode();
		return emissivity * PI
				* (emissionFunction.radianceAt(0.0) + 2.0 * flux(0, nHalf, ordinates.getTotalNodes()));
	}
	
	/**
	 * Calculates the flux at the right boundary using an alternative formula.
	 * @param emissionFunction the emission function
	 * @return the net flux at the right boundary
	 */

	public double fluxRight(final BlackbodySpectrum emissionFunction) {
		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();
		return -emissivity * PI
				* (emissionFunction.radianceAt(grid.getDimension()) - 2.0 * flux(grid.getDensity(), nStart, nHalf));
	}
	
	/**
	 * Calculates the reflected intensity (positive angles, first half of indices)
	 * at the left boundary (&tau; = 0).
	 * @param ef the emission function
	 */

	public void intensitiesLeftBoundary(final BlackbodySpectrum ef) {
		final int nHalf = ordinates.getFirstNegativeNode();
		final int nStart = ordinates.getFirstPositiveNode();

		for (int i = nStart; i < nHalf; i++) {
			// for positive streams
			quantities.setIntensity(0, i, ef.radianceAt(0.0) - boundaryFluxFactor * fluxLeft(ef) );
		}

	}

	/**
	 * Calculates the reflected intensity (negative angles, second half of indices)
	 * at the right boundary (&tau; = &tau;<sub>0</sub>).
	 * @param ef the emission function
	 */

	public void intensitiesRightBoundary(final BlackbodySpectrum ef) {

		final int N = grid.getDensity();
		final int nHalf = ordinates.getFirstNegativeNode();
		final double tau0 = grid.getDimension();
		
		for (int i = nHalf; i < ordinates.getTotalNodes(); i++) {
			// for negative streams
			quantities.setIntensity(N, i, ef.radianceAt(tau0) + boundaryFluxFactor * fluxRight(ef));
		}

	}

	protected void setEmissivity(double emissivity) {
		this.emissivity = emissivity;
		boundaryFluxFactor = (1.0 - emissivity) / (emissivity * PI);
	}
	
	public OrdinateSet getOrdinates() {
		return ordinates;
	}

	public void setOrdinateSet(OrdinateSet set) {
		this.ordinates = set;
		quantities.init(grid.getDensity(), ordinates.getTotalNodes());
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		// intentionally left blank
	}

	@Override
	public List<Property> listedTypes() {
		return new ArrayList<Property>(Arrays.asList(OrdinateSet.defaultSet()));
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

	public StretchedGrid getGrid() {
		return grid;
	}

	public void setGrid(StretchedGrid grid) {
		this.grid = grid;
		this.grid.setParent(this);
	}

	public DiscreteQuantities getQuantities() {
		return quantities;
	}
	
}