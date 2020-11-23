package pulse.problem.statements;

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
import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.AtanhTransform;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.ui.Messages;

public class NonlinearProblem extends ClassicalProblem {

	public NonlinearProblem() {
		super();
		setPulse(new Pulse2D());
		setComplexity(ProblemComplexity.MODERATE);
	}

	public NonlinearProblem(NonlinearProblem p) {
		super(p);
		setPulse(new Pulse2D((Pulse2D) p.getPulse()));
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
	public void optimisationVector(ParameterVector output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		int size = output.dimension();
		var properties = getProperties();

		for (int i = 0; i < size; i++) {

			var key = output.getIndex(i);

			if (key == HEAT_LOSS) {

				var bounds = new Segment(1e-5, properties.maxBiot());
				final double Bi1 = (double) properties.getHeatLoss().getValue();
				output.setTransform(i, new AtanhTransform(bounds));
				output.set(i, Bi1);
				output.setParameterBounds(i, bounds);

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
	public void assign(ParameterVector params) {
		super.assign(params);
		var p = getProperties();

		for (int i = 0, size = params.dimension(); i < size; i++) {

			var key = params.getIndex(i);

			if (key == HEAT_LOSS) {

				p.setHeatLoss(derive(HEAT_LOSS, params.inverseTransform(i)));
				p.emissivity();

			}

		}

	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ImplicitScheme.class;
	}

	@Override
	public Problem copy() {
		return new NonlinearProblem(this);
	}

}