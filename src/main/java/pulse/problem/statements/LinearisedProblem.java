package pulse.problem.statements;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.ImplicitScheme;
import pulse.ui.Messages;

/**
 * The simplest problem statement supported in {@code PULsE}, which is
 * formulated in the dimensionless form and with linearised boundary conditions.
 *
 */

public class LinearisedProblem extends Problem {

	public LinearisedProblem() {
		super();
	}

	public LinearisedProblem(Problem lp) {
		super(lp);
	}

	@Override
	public Class<? extends DifferenceScheme> defaultScheme() {
		return ImplicitScheme.class;
	}

	@Override
	public String toString() {
		return Messages.getString("LinearizedProblem.Descriptor");
	}

}