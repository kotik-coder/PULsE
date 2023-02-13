package pulse.problem.statements;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.DIATHERMIC_COEFFICIENT;

import pulse.math.Parameter;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitDiathermicSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.DiathermicProperties;
import pulse.problem.statements.model.ThermalProperties;
import static pulse.properties.NumericPropertyKeyword.HEAT_LOSS_CONVECTIVE;
import pulse.ui.Messages;

/**
 * The diathermic model is based on the following propositions: - A
 * cylindrically shaped sample is completely transparent to thermal radiation; -
 * The front~(laser-facing) and rear (detector-facing) sides of the sample are
 * coated by a thin grey absorber; - The coatings are in perfect thermal contact
 * with the bulk material; - The side surface is free from any coating.
 * <p>
 * Consequently, the monochromatic laser radiation is largely absorbed at the
 * front face of the sample (y = 0), causing immediate heating. A portion of
 * thermal radiation causes the rear face (y = 1) to start heating precisely at
 * the same time~(ahead of thermal conduction). The remainder energy dissipates
 * in the ambient.
 * </p>
 *
 */
public class DiathermicMedium extends ClassicalProblem {

    private static final long serialVersionUID = -98674255799114512L;

    public DiathermicMedium() {
        super();
    }

    public DiathermicMedium(Problem p) {
        super(p);
    }

    @Override
    public void initProperties() {
        setProperties(new DiathermicProperties());
    }

    @Override
    public void initProperties(ThermalProperties properties) {
        setProperties(new DiathermicProperties(properties));
    }

    @Override
    public void optimisationVector(ParameterVector output) {
        super.optimisationVector(output);
        var properties = (DiathermicProperties) this.getProperties();

        for (Parameter p : output.getParameters()) {

            var key = p.getIdentifier().getKeyword();
            Segment bounds;
            double value;

            switch (key) {
                case DIATHERMIC_COEFFICIENT:
                    bounds = Segment.boundsFrom(DIATHERMIC_COEFFICIENT);
                    value = (double) properties.getDiathermicCoefficient().getValue();
                    break;
                case HEAT_LOSS_CONVECTIVE:
                    bounds = Segment.boundsFrom(HEAT_LOSS_CONVECTIVE);
                    value = (double) properties.getConvectiveLosses().getValue();
                    break;
                case HEAT_LOSS:
                    if (properties.areThermalPropertiesLoaded()) {
                        value = (double) properties.getHeatLoss().getValue();
                        bounds = new Segment(0.0, properties.maxRadiationBiot());
                        break;
                    }
                default:
                    continue;
            }

            p.setTransform(new StickTransform(bounds));
            p.setValue(value);
            p.setBounds(bounds);

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        var properties = (DiathermicProperties) this.getProperties();

        for (Parameter p : params.getParameters()) {

            var key = p.getIdentifier().getKeyword();

            switch (key) {

                case DIATHERMIC_COEFFICIENT:
                    properties.setDiathermicCoefficient(derive(DIATHERMIC_COEFFICIENT,
                            p.inverseTransform()));
                    break;
                case HEAT_LOSS_CONVECTIVE:
                    properties.setConvectiveLosses(derive(HEAT_LOSS_CONVECTIVE,
                            p.inverseTransform()));
                    break;
                default:
            }

        }

    }

    @Override
    public String toString() {
        return Messages.getString("DiathermicProblem.Descriptor");
    }

    @Override
    public Class<? extends DifferenceScheme> defaultScheme() {
        return ImplicitDiathermicSolver.class;
    }

    @Override
    public DiathermicMedium copy() {
        return new DiathermicMedium(this);
    }

}
