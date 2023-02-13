package pulse.problem.statements;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.SPOT_DIAMETER;

import pulse.math.Parameter;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.InvDiamTransform;
import pulse.math.transforms.StickTransform;
import pulse.math.transforms.Transformable;
import pulse.problem.laser.DiscretePulse;
import pulse.problem.laser.DiscretePulse2D;
import pulse.problem.schemes.ADIScheme;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.Grid;
import pulse.problem.schemes.Grid2D;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.ExtendedThermalProperties;
import pulse.problem.statements.model.ThermalProperties;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_SIDE;
import pulse.ui.Messages;

/**
 * The complete problem statement for a fully two-dimensional problem, which
 * includes side heat losses, a variable field of view and variable
 * pulse-to-diameter ratio.
 *
 */
public class ClassicalProblem2D extends ClassicalProblem {

    /**
     *
     */
    private static final long serialVersionUID = 8974995052071820422L;

    public ClassicalProblem2D() {
        super();
        setPulse(new Pulse2D());
        setComplexity(ProblemComplexity.MODERATE);
    }

    public ClassicalProblem2D(Problem p) {
        super(p);
        setPulse(new Pulse2D(p.getPulse()));
        setComplexity(ProblemComplexity.MODERATE);
    }

    @Override
    public void initProperties() {
        setProperties(new ExtendedThermalProperties());
    }

    @Override
    public void initProperties(ThermalProperties properties) {
        setProperties(new ExtendedThermalProperties(properties));
    }

    @Override
    public Class<? extends DifferenceScheme> defaultScheme() {
        return ADIScheme.class;
    }

    @Override
    public String toString() {
        return Messages.getString("LinearizedProblem2D.Descriptor"); //$NON-NLS-1$
    }

    @Override
    public DiscretePulse discretePulseOn(Grid grid) {
        return grid instanceof Grid2D ? new DiscretePulse2D(this, (Grid2D) grid) : super.discretePulseOn(grid);
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);
        var properties = (ExtendedThermalProperties) getProperties();
        double value;

        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();
            Transformable transform = new InvDiamTransform(properties);
            var bounds = Segment.boundsFrom(key);

            switch (key) {
                case FOV_OUTER:
                    value = (double) properties.getFOVOuter().getValue();
                    transform = new StickTransform(bounds);
                    break;
                case FOV_INNER:
                    value = (double) properties.getFOVInner().getValue();
                    break;
                case SPOT_DIAMETER:
                    value = (double) ((Pulse2D) getPulse()).getSpotDiameter().getValue();
                    transform = new StickTransform(bounds);
                    break;
                case HEAT_LOSS_SIDE:
                    value = (double) properties.getSideLosses().getValue();
                    transform = new StickTransform(bounds);
                    break;
                case HEAT_LOSS_COMBINED:
                    value = (double) properties.getHeatLoss().getValue();
                    transform = new StickTransform(bounds);
                    break;
                default:
                    continue;
            }

            p.setTransform(transform);
            p.setBounds(bounds);
            p.setValue(value);

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        var properties = (ExtendedThermalProperties) getProperties();

        // TODO one-to-one mapping for FOV and SPOT_DIAMETER
        for (Parameter p : params.getParameters()) {
            var type = p.getIdentifier().getKeyword();
            switch (type) {
                case FOV_OUTER:
                case FOV_INNER:
                case HEAT_LOSS_SIDE:
                case HEAT_LOSS_COMBINED:
                    properties.set(type, derive(type, p.inverseTransform()));
                    break;
                case SPOT_DIAMETER:
                    ((Pulse2D) getPulse()).setSpotDiameter(derive(SPOT_DIAMETER,
                            p.inverseTransform()));
                    break;
                default:
            }
        }
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public Problem copy() {
        return new ClassicalProblem2D(this);
    }

}
