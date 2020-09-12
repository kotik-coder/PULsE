package pulse.problem.statements;

import static java.lang.Math.PI;
import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.EMISSIVITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.math.IndexedVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.ui.Messages;

public class NonlinearProblem extends Problem {

	private double emissivity = 0.85;

	public NonlinearProblem() {
		super();
		setPulse( new Pulse2D() );
		setComplexity(ProblemComplexity.MODERATE);
	}

	public NonlinearProblem(Problem p) {
		super(p);
		setPulse( new Pulse2D(p.getPulse()));
		setComplexity(ProblemComplexity.MODERATE);
	}

	public NonlinearProblem(NonlinearProblem p) {
		super(p);
		setPulse(new Pulse2D(p.getPulse()));
		this.emissivity = p.emissivity;
		setComplexity(ProblemComplexity.MODERATE);
	}

	@Override
	public void retrieveData(ExperimentalData c) {
		super.retrieveData(c);
		this.setTestTemperature(c.getMetadata().numericProperty(TEST_TEMPERATURE));
	}

	@Override
	public void setSpecificHeat(NumericProperty cV) {
		super.setSpecificHeat(cV);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(TEST_TEMPERATURE));
		list.add(def(SPECIFIC_HEAT));
		list.add(def(DENSITY));
		return list;
	}

	@Override
	public String toString() {
		return Messages.getString("NonlinearProblem.Descriptor");
	}

	public double maximumHeating() {
		final double Q = (double) getPulse().getLaserEnergy().getValue();
		final double dLas = (double) ((Pulse2D) getPulse()).getSpotDiameter().getValue();
		final double l = (double)this.getSampleThickness().getValue();
		final double cP = (double)this.getSpecificHeat().getValue();
		final double rho = (double)this.getDensity().getValue();
		
		return 4.0 * emissivity * Q / (PI * dLas * dLas * l * cP * rho);
	}
	
	public double getEmissivity() {
		return emissivity;
	}

	@Override
	public boolean allDetailsPresent() {
		return areThermalPropertiesLoaded();
	}

	public void evaluateDependentParameters() {
		emissivity = emissivity();
		firePropertyChanged(this, getEmissivityProperty());
	}

	public NumericProperty getThermalConductivity() {
		return derive(CONDUCTIVITY, thermalConductivity());
	}

	public NumericProperty getEmissivityProperty() {
		return derive(EMISSIVITY, emissivity);
	}

	public void setEmissivity(NumericProperty e) {
		requireType(e, EMISSIVITY);
		this.emissivity = (double) e.getValue();
		setHeatLoss( derive(HEAT_LOSS, biot()) );
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		int size = output[0].dimension();

		for (int i = 0; i < size; i++) {

			if (output[0].getIndex(i) == HEAT_LOSS) {
				final double Bi1 = (double)this.getHeatLoss().getValue();
				output[0].set(i, atanh(2.0 * Bi1 / maxBiot() - 1.0));
				output[1].set(i, 10.0);
			}
		}

	}

	@Override
	protected double biot() {
		return biot(emissivity);
	}

	/**
	 * Assigns parameter values of this {@code Problem} using the optimisation
	 * vector {@code params}. Only those parameters will be updated, the types of
	 * which are listed as indices in the {@code params} vector.
	 * 
	 * @param params the optimisation vector, containing a similar set of parameters
	 *               to this {@code Problem}
	 * @see listedTypes()
	 */

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {

			if (params.getIndex(i) == HEAT_LOSS) {
				final double heatLoss = 0.5 * maxBiot() * (tanh(params.get(i)) + 1.0);
				setHeatLoss(derive(HEAT_LOSS, heatLoss));
				emissivity = emissivity();
			}

		}

	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ImplicitScheme.class;
	}

}