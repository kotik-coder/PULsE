package pulse.problem.statements;

import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitLinearisedSolver;
import pulse.problem.statements.model.ThermalProperties;
import pulse.ui.Messages;

/**
 * The simplest problem statement supported in {@code PULsE}, which is
 * formulated in the dimensionless form and with linearised boundary conditions.
 *
 */
public class ClassicalProblem extends Problem {

    public ClassicalProblem() {
        super();
        setPulse(new Pulse());
    }

    public ClassicalProblem(Problem p) {
        super(p);
        setPulse(new Pulse(p.getPulse()));
    }

    @Override
    public Class<? extends DifferenceScheme> defaultScheme() {
        return ImplicitLinearisedSolver.class;
    }

    @Override
    public void initProperties() {
        setProperties(new ThermalProperties());
    }

    @Override
    public void initProperties(ThermalProperties properties) {
        setProperties(new ThermalProperties(properties));
    }

    @Override
    public String toString() {
        return Messages.getString("LinearizedProblem.Descriptor");
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public Problem copy() {
        return new ClassicalProblem(this);
    }

}
