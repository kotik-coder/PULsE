package pulse.problem.statements;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.DIATHERMIC_COEFFICIENT;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import java.util.List;

import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.transforms.StickTransform;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.ImplicitDiathermicSolver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.model.DiathermicProperties;
import pulse.problem.statements.model.ThermalProperties;
import pulse.properties.Flag;
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
    public void optimisationVector(ParameterVector output, List<Flag> flags) {
        super.optimisationVector(output, flags);
        var properties = (DiathermicProperties) this.getProperties();

        for (int i = 0, size = output.dimension(); i < size; i++) {

            var key = output.getIndex(i);

            if (key == DIATHERMIC_COEFFICIENT) {

                var bounds = new Segment(0.0, 1.0);
                final double etta = (double) properties.getDiathermicCoefficient().getValue();

                output.setTransform(i, new StickTransform(bounds));
                output.set(i, etta);
                output.setParameterBounds(i, bounds);

            }

        }

    }

    @Override
    public void assign(ParameterVector params) throws SolverException {
        super.assign(params);
        var properties = (DiathermicProperties) this.getProperties();

        for (int i = 0, size = params.dimension(); i < size; i++) {

            var key = params.getIndex(i);

            switch (key) {

                case DIATHERMIC_COEFFICIENT:
                    properties.setDiathermicCoefficient(derive(DIATHERMIC_COEFFICIENT, params.inverseTransform(i)));
                    break;
                case HEAT_LOSS:
                    if (properties.areThermalPropertiesLoaded()) {
                        properties.calculateEmissivity();
                        final double emissivity = (double) properties.getEmissivity().getValue();
                        properties
                                .setDiathermicCoefficient(derive(DIATHERMIC_COEFFICIENT, emissivity / (2.0 - emissivity)));
                    }
                    break;
                default:
                    continue;

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
