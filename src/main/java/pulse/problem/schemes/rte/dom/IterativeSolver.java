package pulse.problem.schemes.rte.dom;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class IterativeSolver extends PropertyHolder implements Reflexive {

	protected double iterationError;
	private int maxIterations;

	public abstract RTECalculationStatus doIterations(DiscreteIntensities discrete, AdaptiveIntegrator integrator);

	public IterativeSolver() {
		iterationError = (double) NumericProperty.theDefault(NumericPropertyKeyword.DOM_ITERATION_ERROR).getValue();
		maxIterations = (int) NumericProperty.theDefault(NumericPropertyKeyword.RTE_MAX_ITERATIONS).getValue();
	}

	protected RTECalculationStatus sanityCheck(RTECalculationStatus status, int iterations) {
		return iterations < maxIterations ? status : RTECalculationStatus.ITERATION_TIMEOUT;
	}

	public NumericProperty getIterationErrorTolerance() {
		return NumericProperty.derive(NumericPropertyKeyword.DOM_ITERATION_ERROR, this.iterationError);
	}

	public void setIterationErrorTolerance(NumericProperty e) {
		if (e.getType() != NumericPropertyKeyword.DOM_ITERATION_ERROR)
			throw new IllegalArgumentException("Illegal type: " + e.getType());
		this.iterationError = (double) e.getValue();
	}

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case DOM_ITERATION_ERROR:
			setIterationErrorTolerance(property);
			break;
		case RTE_MAX_ITERATIONS:
			setMaxIterations(property);
			break;
		default:
			return;
		}

		notifyListeners(this, property);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(NumericPropertyKeyword.DOM_ITERATION_ERROR));
		list.add(NumericProperty.def(NumericPropertyKeyword.RTE_MAX_ITERATIONS));
		return list;
	}

	public NumericProperty getMaxIterations() {
		return NumericProperty.derive(NumericPropertyKeyword.RTE_MAX_ITERATIONS, maxIterations);
	}

	public void setMaxIterations(NumericProperty iterations) {
		if (iterations.getType() == NumericPropertyKeyword.RTE_MAX_ITERATIONS)
			this.maxIterations = (int) iterations.getValue();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + this.getIterationErrorTolerance();
	}

}