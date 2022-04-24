package pulse.problem.statements;

import static pulse.properties.NumericProperties.derive;

import java.util.List;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.math.transforms.Transformable;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.MixedCoupledSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.ThermalProperties;
import pulse.problem.statements.model.ThermoOpticalProperties;
import pulse.properties.Flag;
import static pulse.properties.NumericPropertyKeyword.OPTICAL_THICKNESS;
import static pulse.properties.NumericPropertyKeyword.PLANCK_NUMBER;
import pulse.ui.Messages;

public class ParticipatingMedium extends NonlinearProblem {

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
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        super.optimisationVector(output, flags);
        var properties = (ThermoOpticalProperties) getProperties();

        Segment bounds = null;
        double value;
        Transformable transform;

        for (int i = 0, size = output.dimension(); i < size; i++) {

            var key = output.getIndex(i);

            switch (key) {
                case PLANCK_NUMBER:
                    final double lowerBound = Segment.boundsFrom(PLANCK_NUMBER).getMinimum();
                    bounds = new Segment(lowerBound, properties.maxNp());
                    value = (double) properties.getPlanckNumber().getValue();
                    break;
                case OPTICAL_THICKNESS:
                    value = (double) properties.getOpticalThickness().getValue();
                    bounds = Segment.boundsFrom(OPTICAL_THICKNESS);
                   break;
                case SCATTERING_ALBEDO:
                    value = (double) properties.getScatteringAlbedo().getValue();
                    bounds = new Segment(0.0, 1.0);
                    break;
                case SCATTERING_ANISOTROPY:
                    value = (double) properties.getScatteringAnisostropy().getValue();
                    bounds = new Segment(-1.0, 1.0);
                    break;
                default:
                    continue;

            }

            transform = new StickTransform(bounds);                
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
