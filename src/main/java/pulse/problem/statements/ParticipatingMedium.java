package pulse.problem.statements;

import static pulse.math.transforms.StandardTransformations.LOG;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import java.util.List;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.AtanhTransform;
import pulse.math.transforms.StickTransform;
import pulse.math.transforms.Transformable;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.MixedCoupledSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.ThermalProperties;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.Flag;
import pulse.ui.Messages;

public class ParticipatingMedium extends NonlinearProblem {

    private final static int DEFAULT_CURVE_POINTS = 300;

    public ParticipatingMedium() {
        super();
        getHeatingCurve().setNumPoints(derive(NUMPOINTS, DEFAULT_CURVE_POINTS));
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
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        super.optimisationVector(output, flags);
        var properties = (ThermoOpticalProperties) getProperties();

        Segment bounds;
        double value = 0;
        Transformable transform;

        for (int i = 0, size = output.dimension(); i < size; i++) {

            var key = output.getIndex(i);

            switch (key) {
                case PLANCK_NUMBER:
                    bounds = new Segment(1E-5, properties.maxNp());
                    value = (double) properties.getPlanckNumber().getValue();
                    transform = new AtanhTransform(bounds);
                    break;
                case OPTICAL_THICKNESS:
                    value = (double) properties.getOpticalThickness().getValue();
                    bounds = new Segment(1E-8, 1E5);
                    transform = LOG;
                    break;
                case SCATTERING_ALBEDO:
                    value = (double) properties.getScatteringAlbedo().getValue();
                    bounds = new Segment(0.0, 1.0);
                    transform = new StickTransform(bounds);
                    break;
                case SCATTERING_ANISOTROPY:
                    value = (double) properties.getScatteringAnisostropy().getValue();
                    bounds = new Segment(-1.0, 1.0);
                    transform = new StickTransform(bounds);
                    break;
                default:
                    continue;

            }

            output.setTransform(i, transform);
            output.set(i, value);
            output.setParameterBounds(i, bounds);

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        var properties = (ThermoOpticalProperties) getProperties();

        for (int i = 0, size = params.dimension(); i < size; i++) {

            var type = params.getIndex(i);

            switch (type) {

                case PLANCK_NUMBER:
                case SCATTERING_ALBEDO:
                case SCATTERING_ANISOTROPY:
                case OPTICAL_THICKNESS:
                    properties.set(type, derive(type, params.inverseTransform(i)));
                    break;
                case HEAT_LOSS:
                case DIFFUSIVITY:
                    properties.emissivity();
                    break;
                default:
                    break;

            }

        }

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
    public Problem copy() {
        return new ParticipatingMedium(this);
    }

}
