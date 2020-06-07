package pulse.problem.statements;

import pulse.ui.Messages;

public class LinearisedProblem2D extends Problem2D {

	public LinearisedProblem2D() {
		super();
	}

	public LinearisedProblem2D(Problem lp2) {
		super(lp2);
	}

	public LinearisedProblem2D(Problem2D lp2) {
		super(lp2);
	}

	@Override
	public String toString() {
		return Messages.getString("LinearizedProblem2D.Descriptor"); //$NON-NLS-1$
	}

}