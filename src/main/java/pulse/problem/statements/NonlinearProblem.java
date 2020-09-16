package pulse.problem.statements;

import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.CONDUCTIVITY;
import static pulse.properties.NumericPropertyKeyword.DENSITY;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS;
import static pulse.properties.NumericPropertyKeyword.SPECIFIC_HEAT;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;
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

public class NonlinearProblem extends ClassicalProblem {

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
	
	@Override
	public boolean isReady() {
		return getProperties().areThermalPropertiesLoaded();
	}

	@Override
	public void retrieveData(ExperimentalData c) {
		super.retrieveData(c);
		getProperties().setTestTemperature(c.getMetadata().numericProperty(TEST_TEMPERATURE));
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(TEST_TEMPERATURE));
		list.add(def(SPECIFIC_HEAT));
		list.add(def(DENSITY));
		list.remove(def(SPOT_DIAMETER));
		return list;
	}

	@Override
	public String toString() {
		return Messages.getString("NonlinearProblem.Descriptor");
	}

	public NumericProperty getThermalConductivity() {
		return derive(CONDUCTIVITY, getProperties().thermalConductivity());
	}
	
	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		int size = output[0].dimension();

		for (int i = 0; i < size; i++) {

			if (output[0].getIndex(i) == HEAT_LOSS) {
				var properties = getProperties();
				final double Bi1 = (double)properties.getHeatLoss().getValue();
				output[0].set(i, atanh(2.0 * Bi1 / properties.maxBiot() - 1.0));
				output[1].set(i, 10.0);
			}
		}

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
		var p = getProperties();
		
		for (int i = 0, size = params.dimension(); i < size; i++) {

			if (params.getIndex(i) == HEAT_LOSS) {
				final double heatLoss = 0.5 * p.maxBiot() * (tanh(params.get(i)) + 1.0);
				p.setHeatLoss(derive(HEAT_LOSS, heatLoss));
				p.emissivity();
			}

		}

	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ImplicitScheme.class;
	}

}