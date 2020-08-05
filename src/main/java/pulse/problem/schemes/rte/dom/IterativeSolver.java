package pulse.problem.schemes.rte.dom;

import static pulse.problem.schemes.rte.RTECalculationStatus.ITERATION_LIMIT_REACHED;
import static pulse.properties.NumericProperty.def;
import static pulse.properties.NumericProperty.derive;
import static pulse.properties.NumericProperty.theDefault;
import static pulse.properties.NumericPropertyKeyword.DOM_ITERATION_ERROR;
import static pulse.properties.NumericPropertyKeyword.RTE_MAX_ITERATIONS;

import java.util.ArrayList;
import java.util.List;

import pulse.problem.schemes.rte.RTECalculationStatus;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * Used to iteratively solve the radiative transfer problem. <p>This is necessary since the latter can
 * only be solved separately for rays travelling in the positive or negative hemispheres. The problem
 * lies in the fact that the intensities of incoming and outward rays are coupled. The only way to 
 * solve the coupled set of two Cauchy problems is iterative in nature.</p> 
 * <p>This abstract class only defines the basic functionaly, its implementation is defined by the 
 * subclasses.</p>
 *
 */

public abstract class IterativeSolver extends PropertyHolder implements Reflexive {

	private double iterationError;
	private int maxIterations;
	
	/**
	 * Constructs an {@code IterativeSolver} with the default thresholds for 
	 * iteration error and number of iterations.
	 */
	
	public IterativeSolver() {
		iterationError = (double) theDefault(DOM_ITERATION_ERROR).getValue();
		maxIterations = (int) theDefault(RTE_MAX_ITERATIONS).getValue();
	}
	
	/**
	 * De-facto solves the radiative transfer problem iteratively.
	 * @param integrator the integerator embedded in the iterative approach
	 * @return a calculation status
	 */
	
	public abstract RTECalculationStatus doIterations(AdaptiveIntegrator integrator);
	
	protected RTECalculationStatus sanityCheck(RTECalculationStatus status, int iterations) {
		return iterations < maxIterations ? status : ITERATION_LIMIT_REACHED;
	}

	public NumericProperty getIterationErrorTolerance() {
		return derive(DOM_ITERATION_ERROR, this.iterationError);
	}

	public void setIterationErrorTolerance(NumericProperty e) {
		if (e.getType() != DOM_ITERATION_ERROR)
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

		firePropertyChanged(this, property);

	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = new ArrayList<>();
		list.add(def(DOM_ITERATION_ERROR));
		list.add(def(RTE_MAX_ITERATIONS));
		return list;
	}

	public NumericProperty getMaxIterations() {
		return derive(RTE_MAX_ITERATIONS, maxIterations);
	}

	public void setMaxIterations(NumericProperty iterations) {
		if (iterations.getType() == RTE_MAX_ITERATIONS)
			this.maxIterations = (int) iterations.getValue();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " : " + this.getIterationErrorTolerance();
	}

	public double getIterationError() {
		return iterationError;
	}

}