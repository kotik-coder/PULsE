package pulse.problem.statements;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.properties.NumericProperty;

public class LinearisedProblem extends Problem {
	
	protected boolean dimensionless = false;
	
	public LinearisedProblem() {
		super();
	}
	
	public LinearisedProblem(Problem lp) {
		super(lp);
	}

	/**
	 * @param dimensionless
	 */
	public LinearisedProblem(NumericProperty curvePoints) {
		super();
		this.curve = new HeatingCurve(curvePoints);
	}
	
	public double timeFactor() {
		return dimensionless ? 1.0 : super.timeFactor();
	}
	
	@Override
	public DifferenceScheme[] availableSolutions() {
		return new DifferenceScheme[]{new ExplicitScheme(), new ImplicitScheme(), new MixedScheme()};
	}
	
	@Override
	public String toString() {
		return Messages.getString("LinearizedProblem.Descriptor"); //$NON-NLS-1$
	}

}
