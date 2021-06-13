package pulse.search.direction;

import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperties.isDiscrete;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.GRADIENT_RESOLUTION;

import java.util.List;

import pulse.math.ParameterVector;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericProperties;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.SearchTask;

public abstract class GradientBasedOptimiser extends PathOptimiser {

	private double gradientResolution;
	private double gradientStep;

	/**
	 * Abstract constructor that sets up the default
	 * {@code ITERATION_LIMIT, ERROR_TOLERANCE} and {@code GRADIENT_RESOLUTION} for
	 * this {@code PathSolver}. In addition, sets up a list of search flags defined
	 * by the {@code Flag.defaultList} method.
	 * 
	 * @see pulse.properties.Flag.defaultList()
	 */

	protected GradientBasedOptimiser() {
		super();
	}

	/**
	 * Resets the default {@code ITERATION_LIMIT, ERROR_TOLERANCE} and
	 * {@code GRADIENT_RESOLUTION} values for this {@code PathSolver}. In addition,
	 * sets up a list of search flags defined by the {@code Flag.defaultList}
	 * method.
	 * 
	 * @see pulse.properties.Flag.defaultList()
	 */

	public void reset() {
		super.reset();
		gradientResolution = (double) def(GRADIENT_RESOLUTION).getValue();
	}

	/**
	 * Calculates the {@code Vector} gradient of the target function (the sum of
	 * squared residuals, SSR, for this {@code task}.
	 * <p>
	 * If <math><i>&Delta;f(&Delta;x<sub>i</sub>)</i></math> is the change in the
	 * target function associated with the change of the parameter
	 * <math><i>x<sub>i</sub></i></math>, the <i>i</i>-th component of the gradient
	 * is equal to <math><i>g<sub>i</sub> =
	 * (&Delta;f(&Delta;x<sub>i</sub>)/&Delta;x<sub>i</sub>)</i></math>. The
	 * accuracy of this calculation depends on the
	 * <math><i>&Delta;x<sub>i</sub></i></math> value, which is roughly the
	 * {@code GRADIENT_RESOLUTION}. Note however that instead of using a
	 * forward-difference scheme to calculate the gradient, this method utilises the
	 * central-difference calculation of the gradient, which significantly increases
	 * the overall accuracy of calculation. This means that to evaluate each
	 * component of this vector, the {@code Problem} associated with this
	 * {@code task} is solved twice (for <math><i>x<sub>i</sub> &pm;
	 * &Delta;x<sub>i</sub></i></math>).
	 * </p>
	 * 
	 * @param task a {@code SearchTask} that is being driven to the minimum of SSR
	 * @return the gradient of the target function
	 * @throws SolverException
	 */

	public Vector gradient(SearchTask task) throws SolverException {

		final var params = task.searchVector();
		var grad = new Vector(params.dimension());


		final double resolutionHigh = (double) getGradientResolution().getValue();
		final double resolutionLow  = 5E-2; //TODO 
		
		for (int i = 0; i < params.dimension(); i++) {
			boolean discrete = NumericProperties.def(params.getIndex(i)).isDiscrete();
			double dx		 = (discrete ? resolutionLow : resolutionHigh) * params.get(i);			
			
			final var shift = new Vector(params.dimension());
			shift.set(i, 0.5 * dx);

			task.assign(new ParameterVector( params, params.sum(shift) ));
			final double ss2 = task.solveProblemAndCalculateCost();

			task.assign(new ParameterVector( params, params.subtract(shift) ));
			final double ss1 = task.solveProblemAndCalculateCost();

			grad.set(i, (ss2 - ss1) / dx);

		}

		task.assign(params);

		return grad;

	}
	
	/**
	 * Checks whether a discrete property is being optimised and selects the gradient step
	 * best suited to the optimisation strategy. Should be called before creating the optimisation path.
	 * @param task the search task defining the search vector
	 */
	
	public void configure(SearchTask task) {
		var params = task.searchVector();
		boolean discreteGradient = params.getIndices().stream().anyMatch(index -> isDiscrete(index));
		final double dxGrid = task.getCurrentCalculation().getScheme().getGrid().getXStep();
		gradientStep = discreteGradient ? dxGrid : (double) getGradientResolution().getValue();
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
	 * The types of the listed parameters for this class include:
	 * <code> GRADIENT_RESOLUTION,
	 * ERROR_TOLERANCE, ITERATION_LIMIT</code>. Also, all the flags in this class
	 * are treated as separate listed parameters.
	 * </p>
	 * 
	 * @see pulse.properties.NumericPropertyKeyword
	 */

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(def(GRADIENT_RESOLUTION));
		return list;
	}

	/**
	 * The accepted types are:
	 * <code> GRADIENT_RESOLUTION, ERROR_TOLERANCE, ITERATION_LIMIT</code>.
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		super.set(type, property);
		if(type == GRADIENT_RESOLUTION) {
			setGradientResolution(property);
		}
	}

	public double getGradientStep() {
		return gradientStep;
	}
	
}