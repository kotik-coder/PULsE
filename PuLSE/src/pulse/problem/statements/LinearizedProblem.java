package pulse.problem.statements;

import pulse.HeatingCurve;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ExplicitScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.problem.schemes.MixedScheme;
import pulse.properties.BooleanProperty;
import pulse.properties.NumericProperty;

public class LinearizedProblem extends Problem {
	
	protected boolean dimensionless = false;
	
	public LinearizedProblem() {
		super();
	}
	
	public LinearizedProblem(Problem lp) {
		super(lp);
	}

	/**
	 * @param dimensionless
	 */
	public LinearizedProblem(NumericProperty curvePoints, BooleanProperty dimensionless) {
		super();
		this.dimensionless = (Boolean)dimensionless.getValue();
		this.curve = new HeatingCurve(curvePoints);
	}

	public BooleanProperty isDimensionless() {
		return new BooleanProperty(Messages.getString("LinearizedProblem.0"), dimensionless); //$NON-NLS-1$
	}

	public void setDimensionless(BooleanProperty dimensionless) {
		this.dimensionless = (Boolean)dimensionless.getValue();
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
