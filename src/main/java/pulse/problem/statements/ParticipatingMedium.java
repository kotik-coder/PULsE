package pulse.problem.statements;

import java.util.Set;

import pulse.math.ParameterVector;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.MixedCoupledSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.ThermalProperties;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.SOURCE_GEOMETRIC_FACTOR;
import pulse.ui.Messages;

public class ParticipatingMedium extends NonlinearProblem {

    private static final long serialVersionUID = -8227061869299826343L;

    public ParticipatingMedium() {
        super();
        setComplexity(ProblemComplexity.HIGH);
    }

    public ParticipatingMedium(ParticipatingMedium p) {
        super(p);
        setComplexity(ProblemComplexity.HIGH);
    }

    @Override
    public String toString() {
        return Messages.getString("ParticipatingMedium.Descriptor");
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);
        var properties = (ThermoOpticalProperties) getProperties();
        properties.optimisationVector(output);
    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        var properties = (ThermoOpticalProperties) getProperties();
        properties.assign(params);
    }

    @Override
    public Class<? extends DifferenceScheme> defaultScheme() {
        return MixedCoupledSolver.class;
    }

    @Override
    public void initProperties(ThermalProperties properties) {
        setProperties(new ThermoOpticalProperties(properties));
    }

    @Override
    public void initProperties() {
        setProperties(new ThermoOpticalProperties());
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(SOURCE_GEOMETRIC_FACTOR);
        return set;
    }

    @Override
    public Problem copy() {
        return new ParticipatingMedium(this);
    }

}
