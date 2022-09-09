package pulse.search.direction;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.GRADIENT_RESOLUTION;

import java.util.Set;

import pulse.math.ParameterVector;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.GeneralTask;

public abstract class GradientBasedOptimiser extends PathOptimiser {

    private double gradientResolution;
    private double gradientStep;

    private final static double RESOLUTION_HIGH = (double) def(GRADIENT_RESOLUTION).getValue();
    private final static double RESOLUTION_LOW = 5E-2; //TODO 

    /**
     * Abstract constructor that sets up the default
     * {@code ITERATION_LIMIT, ERROR_TOLERANCE} and {@code GRADIENT_RESOLUTION}
     * for this {@code PathSolver}. In addition, sets up a list of search flags
     * defined by the {@code Flag.defaultList} method.
     *
     * @see pulse.properties.Flag.defaultList()
     */
    protected GradientBasedOptimiser() {
        super();
        this.gradientResolution = gradientStep = RESOLUTION_HIGH;
    }

    /**
     * Resets the default {@code ITERATION_LIMIT, ERROR_TOLERANCE} and
     * {@code GRADIENT_RESOLUTION} values for this {@code PathSolver}. In
     * addition, sets up a list of search flags defined by the
     * {@code Flag.defaultList} method.
     *
     * @see pulse.properties.Flag.defaultList()
     */
    @Override
    public void reset() {
        super.reset();
        gradientResolution = RESOLUTION_HIGH;
        gradientStep = gradientResolution;
    }

    /**
     * Calculates the {@code Vector} gradient of the target function (the sum of
     * squared residuals, SSR, for this {@code task}.
     * <p>
     * If <math><i>&Delta;f(&Delta;x<sub>i</sub>)</i></math> is the change in
     * the target function associated with the change of the parameter
     * <math><i>x<sub>i</sub></i></math>, the <i>i</i>-th component of the
     * gradient is equal to <math><i>g<sub>i</sub> =
     * (&Delta;f(&Delta;x<sub>i</sub>)/&Delta;x<sub>i</sub>)</i></math>. The
     * accuracy of this calculation depends on the
     * <math><i>&Delta;x<sub>i</sub></i></math> value, which is roughly the
     * {@code GRADIENT_RESOLUTION}. Note however that instead of using a
     * forward-difference scheme to calculate the gradient, this method utilises
     * the central-difference calculation of the gradient, which significantly
     * increases the overall accuracy of calculation. This means that to
     * evaluate each component of this vector, the {@code Problem} associated
     * with this {@code task} is solved twice (for <math><i>x<sub>i</sub> &pm;
     * &Delta;x<sub>i</sub></i></math>).
     * </p>
     *
     * @param task a {@code SearchTask} that is being driven to the minimum of
     * SSR
     * @return the gradient of the target function
     * @throws SolverException
     */
    public Vector gradient(GeneralTask task) throws SolverException {

        final var params = task.searchVector();
        final var pVector = params.toVector();
        var grad = new Vector(params.dimension());
        final var ps = params.getParameters();
        
        for (int i = 0, size = params.dimension(); i < size; i++) {
            var key = ps.get(i).getIdentifier().getKeyword();
            var defProp = key != null ? NumericProperties.def(key) : null;
            double dx = dx(defProp, ps.get(i).inverseTransform());

            final var shift = new Vector(params.dimension());
            shift.set(i, 0.5 * dx);

            var shiftVector = new ParameterVector(params, pVector.sum(shift));
            task.assign(shiftVector);
            final double ss2 = task.objectiveFunction();

            task.assign(new ParameterVector(params, pVector.subtract(shift)));
            final double ss1 = task.objectiveFunction();

            grad.set(i, (ss2 - ss1) / dx);
        }

        task.assign(params);

        return grad;

    }

    /**
     * Calculates the gradient step. Ensures dx is not zero even if the
     * parameter values is. Applicable to discrete properties.
     *
     * @param defProp the default property
     * @param value the value of the parameter under the optimisation vector
     * @return the gradient step
     */
    protected double dx(NumericProperty defProp, double value) {
        double result;

        if (defProp == null) {
            result = gradientResolution * (Math.abs(value) < 1E-20 ? 0.01 : value);
        } else {
            boolean discrete = defProp.isDiscrete();
            result = (discrete ? RESOLUTION_LOW : gradientResolution)
                    * (Math.abs(value) < 1E-20
                    ? defProp.getMaximum().doubleValue()
                    : value);
        }

        return result;
    }

    public void setGradientResolution(NumericProperty resolution) {
        requireType(resolution, GRADIENT_RESOLUTION);
        this.gradientResolution = (double) resolution.getValue();
        firePropertyChanged(this, resolution);
    }

    public NumericProperty getGradientResolution() {
        return derive(GRADIENT_RESOLUTION, gradientResolution);
    }

    /**
     * <p>
     * The types of the listed parameters for this class include:      <code> GRADIENT_RESOLUTION,
     * ERROR_TOLERANCE, ITERATION_LIMIT</code>. Also, all the flags in this
     * class are treated as separate listed parameters.
     * </p>
     *
     * @see pulse.properties.NumericPropertyKeyword
     */
    @Override
    public Set<NumericPropertyKeyword> listedKeywords() {
        var set = super.listedKeywords();
        set.add(GRADIENT_RESOLUTION);
        return set;
    }

    /**
     * The accepted types are:
     * <code> GRADIENT_RESOLUTION, ERROR_TOLERANCE, ITERATION_LIMIT</code>.
     */
    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        super.set(type, property);
        if (type == GRADIENT_RESOLUTION) {
            setGradientResolution(property);
        }
    }

    public double getGradientStep() {
        return gradientStep;
    }

}
