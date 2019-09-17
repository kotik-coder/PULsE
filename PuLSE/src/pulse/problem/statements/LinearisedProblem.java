package pulse.problem.statements;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.properties.NumericProperty;
import pulse.ui.Messages;

public class LinearisedProblem extends Problem {
	
	public LinearisedProblem() {
		super();
	}
	
	public LinearisedProblem(Problem lp) {
		super(lp);
	}

	public LinearisedProblem(NumericProperty curvePoints) {
		super();
		this.curve = new HeatingCurve(curvePoints);
	}
	
	@Override
	public String toString() {
		return Messages.getString("LinearizedProblem.Descriptor"); 
	}

}