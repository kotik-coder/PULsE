package pulse.problem.statements;

import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericPropertyKeyword.DIATHERMIC_COEFFICIENT;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import java.util.List;
import static java.lang.Math.*;

import pulse.math.IndexedVector;
import static pulse.math.MathUtils.*;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
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

public class DiathermicMedium extends LinearisedProblem {

	private double diathermicCoefficient;
	private final static int DEFAULT_CURVE_POINTS = 300;

	public DiathermicMedium() {
		this(def(DIATHERMIC_COEFFICIENT));
	}

	public DiathermicMedium(NumericProperty diathermicCoefficient) {
		super();
		this.diathermicCoefficient = (double) (diathermicCoefficient.getValue());
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
	}

	public DiathermicMedium(Problem sdd) {
		super(sdd);
		this.diathermicCoefficient = sdd instanceof DiathermicMedium ? ((DiathermicMedium) sdd).diathermicCoefficient
				: (double) def(DIATHERMIC_COEFFICIENT).getValue();
		getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
	}

	public NumericProperty getDiathermicCoefficient() {
		return derive(DIATHERMIC_COEFFICIENT, diathermicCoefficient);
	}

	public void setDiathermicCoefficient(NumericProperty diathermicCoefficient) {
		this.diathermicCoefficient = (double) diathermicCoefficient.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == DIATHERMIC_COEFFICIENT) {
			diathermicCoefficient = ((Number) property.getValue()).doubleValue();
		} else {
			super.set(type, property);
		}
	}

	@Override
	public void optimisationVector(IndexedVector[] output, List<Flag> flags) {
		super.optimisationVector(output, flags);

		for (int i = 0, size = output[0].dimension(); i < size; i++) {
			if (output[0].getIndex(i) == DIATHERMIC_COEFFICIENT) {
				output[0].set(i, atanh(2.0 * diathermicCoefficient - 1.0));
				output[1].set(i, 10.0);
			}
		}

	}

	@Override
	public void assign(IndexedVector params) {
		super.assign(params);

		for (int i = 0, size = params.dimension(); i < size; i++) {
			switch (params.getIndex(i)) {
			case DIATHERMIC_COEFFICIENT:
				diathermicCoefficient = 0.5 * (tanh(params.get(i)) + 1.0);
				break;
			case HEAT_LOSS:
				if (areThermalPropertiesLoaded()) {
					final double emissivity = emissivity();
					diathermicCoefficient = emissivity / (2.0 - emissivity);
				}
				break;
			default:
				continue;
			}
		}
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(DIATHERMIC_COEFFICIENT));
		return list;
	}

	@Override
	public String toString() {
		return Messages.getString("DiathermicProblem.Descriptor");
	}

}