package pulse.problem.schemes.rte.dom;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

public abstract class IterativeSolver extends PropertyHolder implements Reflexive {

	protected double iterationError;

	public abstract void doIterations(DiscreteIntensities discrete, AdaptiveIntegrator integrator);

	public IterativeSolver() {
		iterationError = (double) NumericProperty.theDefault(NumericPropertyKeyword.DOM_ITERATION_ERROR).getValue();
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
		default:
			return;
		}

		notifyListeners(this, property);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<Property>();
		list.add(NumericProperty.def(NumericPropertyKeyword.DOM_ITERATION_ERROR));
		return list;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + this.getIterationErrorTolerance();
	}

}