package pulse.problem.statements;

import java.util.Set;
import pulse.math.Parameter;
import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitLinearisedSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.ThermalProperties;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import pulse.properties.NumericProperty;
import static pulse.properties.NumericProperty.requireType;
import pulse.properties.NumericPropertyKeyword;
import static pulse.properties.NumericPropertyKeyword.SOURCE_GEOMETRIC_FACTOR;
import pulse.ui.Messages;

/**
 * The simplest problem statement supported in {@code PULsE}, which is
 * formulated in the dimensionless form and with linearised boundary conditions.
 *
 */
public class ClassicalProblem extends Problem {

    private double bias;

    public ClassicalProblem() {
        super();
        bias = (double) def(SOURCE_GEOMETRIC_FACTOR).getValue();
        setPulse(new Pulse());
    }

    public ClassicalProblem(Problem p) {
        super(p);
        bias = (double) def(SOURCE_GEOMETRIC_FACTOR).getValue();
        setPulse(new Pulse(p.getPulse()));
    }

    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(SOURCE_GEOMETRIC_FACTOR);
        return set;
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

    public NumericProperty getGeometricFactor() {
        return derive(SOURCE_GEOMETRIC_FACTOR, bias);
    }

    public void setGeometricFactor(NumericProperty bias) {
        requireType(bias, SOURCE_GEOMETRIC_FACTOR);
        this.bias = (double) bias.getValue();
        firePropertyChanged(this, bias);
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty value) {
        super.set(type, value);
        if (type == SOURCE_GEOMETRIC_FACTOR) {
            setGeometricFactor(value);
        }
    }

    @Override
    public void optimisationVector(ParameterVector output) {

        super.optimisationVector(output);

        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();

            if (key == SOURCE_GEOMETRIC_FACTOR) {
                var bounds = Segment.boundsFrom(SOURCE_GEOMETRIC_FACTOR);
                p.setTransform(new StickTransform(bounds));
                p.setValue(bias);
            }

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        for (Parameter p : params.getParameters()) {

            double value = p.inverseTransform();
            var key = p.getIdentifier().getKeyword();

            if (key == SOURCE_GEOMETRIC_FACTOR) {
                setGeometricFactor(derive(SOURCE_GEOMETRIC_FACTOR, value));
            }

        }

    }

}
