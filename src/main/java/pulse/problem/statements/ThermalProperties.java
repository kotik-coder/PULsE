package pulse.problem.statements;

import static java.lang.Math.PI;
import static pulse.input.InterpolationDataset.getDataset;
import static pulse.input.InterpolationDataset.StandartType.HEAT_CAPACITY;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.DIFFUSIVITY;
import static pulse.properties.NumericPropertyKeyword.EMISSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.MAXTEMP;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import static pulse.properties.NumericPropertyKeyword.THICKNESS;

import java.util.ArrayList;
import java.util.List;

import pulse.input.ExperimentalData;
import pulse.input.InterpolationDataset.StandartType;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

public class ThermalProperties extends PropertyHolder {

	private double a;
	private double l;
	private double Bi;
	private double signalHeight;
	private double cP;
	private double rho;
	private double T;
	private double emissivity;

	public final static double STEFAN_BOTLZMAN = 5.6703E-08; // Stephan-Boltzmann constant

	/**
	 * The <b>corrected</b> proportionality factor setting out the relation between
	 * the thermal diffusivity and the half-rise time of an {@code ExperimentalData}
	 * curve.
	 * 
	 * @see <a href="https://doi.org/10.1063/1.1728417">Parker <i>et al.</i> Journal
	 *      of Applied Physics <b>32</b> (1961) 1679</a>
	 * @see <a href="https://doi.org/10.1016/j.ces.2019.01.014">Parker <i>et al.</i>
	 *      Chem. Eng. Sci. <b>199</b> (2019) 546-551</a>
	 */

	public final double PARKERS_COEFFICIENT = 0.1370; // in mm

	public ThermalProperties() {
		super();
		a = (double) def(DIFFUSIVITY).getValue();
		l = (double) def(THICKNESS).getValue();
		Bi = (double) def(HEAT_LOSS).getValue();
		signalHeight = (double) def(MAXTEMP).getValue();
		T = (double) def(TEST_TEMPERATURE).getValue();
		emissivity = (double) def(EMISSIVITY).getValue();
	}

	public ThermalProperties(ThermalProperties p) {
		super();
		this.l = p.l;
		this.a = p.a;
		this.Bi = p.Bi;
		this.T = p.T;
		this.emissivity = p.emissivity;
	}

	public ThermalProperties copy() {
		return new ThermalProperties(this);
	}

	/**
	 * Used to change the parameter values of this {@code Problem}. It is only
	 * allowed to use those types of {@code NumericPropery} that are listed by the
	 * {@code listedParameters()}.
	 * 
	 * @see listedTypes()
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty value) {
		switch (type) {
		case DIFFUSIVITY:
			setDiffusivity(value);
			break;
		case MAXTEMP:
			setMaximumTemperature(value);
			break;
		case THICKNESS:
			setSampleThickness(value);
			break;
		case HEAT_LOSS:
			setHeatLoss(value);
			break;
		case SPECIFIC_HEAT:
			setSpecificHeat(value);
			break;
		case DENSITY:
			setDensity(value);
			break;
		case TEST_TEMPERATURE:
			setTestTemperature(value);
			break;
		default:
			break;
		}
	}

	public void setHeatLoss(NumericProperty Bi) {
		requireType(Bi, HEAT_LOSS);
		this.Bi = (double) Bi.getValue();
		firePropertyChanged(this, Bi);
	}

	public NumericProperty getDiffusivity() {
		return derive(DIFFUSIVITY, a);
	}

	public void setDiffusivity(NumericProperty a) {
		requireType(a, DIFFUSIVITY);
		this.a = (double) a.getValue();
		firePropertyChanged(this, a);
	}

	public NumericProperty getMaximumTemperature() {
		return derive(MAXTEMP, signalHeight);
	}

	public void setMaximumTemperature(NumericProperty maxTemp) {
		requireType(maxTemp, MAXTEMP);
		this.signalHeight = (double) maxTemp.getValue();
		firePropertyChanged(this, maxTemp);
	}

	public NumericProperty getSampleThickness() {
		return derive(THICKNESS, l);
	}

	public void setSampleThickness(NumericProperty l) {
		requireType(l, THICKNESS);
		this.l = (double) l.getValue();
		firePropertyChanged(this, l);
	}

	/**
	 * <p>
	 * Assuming that <code>Bi<sub>1</sub> = Bi<sub>2</sub></code>, returns the value
	 * of <code>Bi<sub>1</sub></code>. If
	 * <code>Bi<sub>1</sub> = Bi<sub>2</sub></code>, this will print a warning
	 * message (but will not throw an exception)
	 * </p>
	 * 
	 * @return Bi<sub>1</sub> as a {@code NumericProperty}
	 */

	public NumericProperty getHeatLoss() {
		return derive(HEAT_LOSS, Bi);
	}

	public NumericProperty getSpecificHeat() {
		return derive(SPECIFIC_HEAT, cP);
	}

	public void setSpecificHeat(NumericProperty cP) {
		requireType(cP, SPECIFIC_HEAT);
		this.cP = (double) cP.getValue();
	}

	public NumericProperty getDensity() {
		return derive(DENSITY, rho);
	}

	public void setDensity(NumericProperty p) {
		requireType(p, DENSITY);
		this.rho = (double) (p.getValue());
	}

	public NumericProperty getTestTemperature() {
		return derive(TEST_TEMPERATURE, T);
	}

	public void setTestTemperature(NumericProperty T) {
		requireType(T, TEST_TEMPERATURE);
		this.T = (double) T.getValue();

		var heatCapacity = getDataset(HEAT_CAPACITY);

		if (heatCapacity != null)
			cP = heatCapacity.interpolateAt(this.T);

		var density = getDataset(StandartType.DENSITY);

		if (density != null)
			rho = density.interpolateAt(this.T);

		firePropertyChanged(this, T);
	}

	/**
	 * Listed parameters include:
	 * <code>MAXTEMP, DIFFUSIVITY, THICKNESS, HEAT_LOSS_FRONT, HEAT_LOSS_REAR</code>.
	 */

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(def(MAXTEMP));
		list.add(def(DIFFUSIVITY));
		list.add(def(THICKNESS));
		list.add(def(HEAT_LOSS));
		list.add(def(DENSITY));
		list.add(def(SPECIFIC_HEAT));
		return list;
	}

	public final double thermalConductivity() {
		return a * cP * rho;
	}

	public void emissivity() {
		setEmissivity(derive(EMISSIVITY, Bi * thermalConductivity() / (4. * Math.pow(T, 3) * l * STEFAN_BOTLZMAN)));
	}

	protected double maxBiot() {
		double lambda = thermalConductivity();
		return 4.0 * STEFAN_BOTLZMAN * Math.pow(T, 3) * l / lambda;
	}

	protected double biot() {
		double lambda = thermalConductivity();
		return 4.0 * emissivity * STEFAN_BOTLZMAN * Math.pow(T, 3) * l / lambda;
	}

	/**
	 * Performs simple calculation of the <math><i>l<sup>2</sup>/a</i></math> factor
	 * that is commonly used to evaluate the dimensionless time
	 * {@code t/timeFactor}.
	 * 
	 * @return the time factor
	 */

	public double timeFactor() {
		return l * l / a;
	}

	/**
	 * Calculates the half-rise time <i>t</i><sub>1/2</sub> of {@code c} and uses it
	 * to estimate the thermal diffusivity of this problem:
	 * <code><i>a</i>={@value PARKERS_COEFFICIENT}*<i>l</i><sup>2</sup>/<i>t</i><sub>1/2</sub></code>.
	 * 
	 * @param c the {@code ExperimentalData} used to estimate the thermal
	 *          diffusivity value
	 * @see pulse.input.ExperimentalData.halfRiseTime()
	 */

	public void useTheoreticalEstimates(ExperimentalData c) {
		final double t0 = c.halfRiseTime();
		this.a = PARKERS_COEFFICIENT * l * l / t0;
		if (areThermalPropertiesLoaded())
			Bi = biot();
	}

	public final boolean areThermalPropertiesLoaded() {
		return (Double.compare(cP, 0.0) > 0 && Double.compare(rho, 0.0) > 0);
	}

	public double maximumHeating(Pulse2D pulse) {
		final double Q = (double) pulse.getLaserEnergy().getValue();
		final double dLas = (double) pulse.getSpotDiameter().getValue();
		return 4.0 * emissivity * Q / (PI * dLas * dLas * l * cP * rho);
	}

	public NumericProperty getEmissivity() {
		return derive(EMISSIVITY, emissivity);
	}

	public void setEmissivity(NumericProperty e) {
		requireType(e, EMISSIVITY);
		this.emissivity = (double) e.getValue();
		setHeatLoss(derive(HEAT_LOSS, biot()));
	}
	
	@Override
	public String getDescriptor() {
		return "Sample Thermo-Physical Properties";
	}
	
	@Override
	public String toString() {
		return "Show Details...";
	}

}