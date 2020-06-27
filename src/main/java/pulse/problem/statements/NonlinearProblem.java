package pulse.problem.statements;

import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;

import java.util.List;

import pulse.input.ExperimentalData;
import pulse.math.IndexedVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.rte.MathUtils;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.ui.Messages;

public class NonlinearProblem extends Problem {

	protected double emissivity = 0.85;

	private final static boolean DEBUG = false;

	public NonlinearProblem() {
		super();
		pulse = new Pulse2D();
		setComplexity(ProblemComplexity.MODERATE);
	}

	public NonlinearProblem(Problem p) {
		super(p);
		pulse = new Pulse2D(p.getPulse());
		setComplexity(ProblemComplexity.MODERATE);
	}

	public NonlinearProblem(NonlinearProblem p) {
		super(p);
		pulse = new Pulse2D(p.getPulse());
		setComplexity(ProblemComplexity.MODERATE);
	}

	@Override
	public void retrieveData(ExperimentalData c) {
		super.retrieveData(c);
		this.setTestTemperature(c.getMetadata().getTestTemperature());
	}

	@Override
	public void setSpecificHeat(NumericProperty cV) {
		super.setSpecificHeat(cV);
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(NumericProperty.def(TEST_TEMPERATURE));
		list.add(NumericProperty.def(SPECIFIC_HEAT));
		list.add(NumericProperty.def(DENSITY));
		return list;
	}

	@Override
	public String toString() {
		return Messages.getString("NonlinearProblem.Descriptor");
	}

	@Override
	public boolean isEnabled() {
		return !DEBUG;
	}

	public double maximumHeating() {
		double Q = (double) pulse.getLaserEnergy().getValue();
		double dLas = (double) ((Pulse2D) pulse).getSpotDiameter().getValue();

		evaluateDependentParameters();

		return 4.0 * emissivity * Q / (Math.PI * dLas * dLas * l * cP * rho);
	}

	@Override
	public boolean allDetailsPresent() {
		return areThermalPropertiesLoaded();
	}

	public void evaluateDependentParameters() {
		emissivity = emissivity();
		notifyListeners(this, getEmissivityProperty());
	}

	public NumericProperty getThermalConductivity() {
		return NumericProperty.derive(CONDUCTIVITY, thermalConductivity());
	}

	public NumericProperty getEmissivityProperty() {
		return NumericProperty.derive(NumericPropertyKeyword.EMISSIVITY, emissivity);
	}

	public void setEmissivity(NumericProperty e) {
		if (e.getType() != NumericPropertyKeyword.EMISSIVITY)
			throw new IllegalArgumentException("Illegal type: " + e.getType());
		this.emissivity = (double) e.getValue();
		Bi1 = biot();
		Bi2 = Bi1;
	}

	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		int size = output[0].dimension();

		for (int i = 0; i < size; i++) {

			if (output[0].getIndex(i) == NumericPropertyKeyword.HEAT_LOSS) {
				output[0].set(i, MathUtils.atanh(2.0 * Bi1 / maxBiot() - 1.0));
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

	public void assign(IndexedVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {

			if (params.getIndex(i) == NumericPropertyKeyword.HEAT_LOSS) {
				double heatLoss = 0.5 * maxBiot() * (Math.tanh(params.get(i)) + 1.0);
				Bi1 = heatLoss;
				Bi2 = heatLoss;
			}

		}

	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ImplicitScheme.class;
	}

}