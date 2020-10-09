package pulse.problem.statements;

import static java.lang.Math.tanh;
import static pulse.math.MathUtils.atanh;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.DIATHERMIC_COEFFICIENT;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitDiathermicSolver;
import pulse.problem.statements.model.DiathermicProperties;
import pulse.problem.statements.model.ThermalProperties;
import pulse.properties.Flag;
import pulse.ui.Messages;

/**
 * The diathermic model is based on the following propositions: - A
 * cylindrically shaped sample is completely transparent to thermal radiation; -
 * The front~(laser-facing) and rear (detector-facing) sides of the sample are
 * coated by a thin grey absorber; - The coatings are in perfect thermal contact
 * with the bulk material; - The side surface is free from any coating.
 * <p>
 * Consequently, the monochromatic laser radiation is largely absorbed at the
 * front face of the sample (y = 0), causing immediate heating. A portion of
 * thermal radiation causes the rear face (y = 1) to start heating precisely at
 * the same time~(ahead of thermal conduction). The remainder energy dissipates
 * in the ambient.
 * </p>
 *
 */

public class DiathermicMedium extends ClassicalProblem {

	private final static int DEFAULT_CURVE_POINTS = 300;

	public DiathermicMedium() {
		super();
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
	}

	public DiathermicMedium(Problem p) {
		super(p);
	}
	
	@Override
	public void initProperties() {
		setProperties(new DiathermicProperties());
	}
	
	@Override
	public void initProperties(ThermalProperties properties) {
		setProperties(new DiathermicProperties(properties));
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);
		var properties = (DiathermicProperties) this.getProperties();

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			if (output[0].getIndex(i) == DIATHERMIC_COEFFICIENT) {
				final double etta = (double) properties.getDiathermicCoefficient().getValue();
				output[0].set(i, atanh(2.0 * etta - 1.0));
				output[1].set(i, 10.0);
			}
		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);
		var properties = (DiathermicProperties) this.getProperties();

		for (int i = 0, size = params.dimension(); i < size; i++) {
			switch (params.getIndex(i)) {
			case DIATHERMIC_COEFFICIENT:
				properties.setDiathermicCoefficient(derive(DIATHERMIC_COEFFICIENT, 0.5 * (tanh(params.get(i)) + 1.0)));
				break;
			case HEAT_LOSS:
				if (properties.areThermalPropertiesLoaded()) {
					properties.emissivity();
					final double emissivity = (double) properties.getEmissivity().getValue();
					properties
							.setDiathermicCoefficient(derive(DIATHERMIC_COEFFICIENT, emissivity / (2.0 - emissivity)));
				}
				break;
			default:
				continue;
			}
		}
	}

	@Override
	public String toString() {
		return Messages.getString("DiathermicProblem.Descriptor");
	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ImplicitDiathermicSolver.class;
	}

	@Override
	public DiathermicMedium copy() {
		return new DiathermicMedium(this);
	}

}