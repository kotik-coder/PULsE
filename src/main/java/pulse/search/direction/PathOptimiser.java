package pulse.search.direction;

import static pulse.properties.NumericPropertyKeyword.ERROR_TOLERANCE;
import static pulse.properties.NumericPropertyKeyword.GRADIENT_RESOLUTION;
import static pulse.properties.NumericPropertyKeyword.ITERATION_LIMIT;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pulse.algebra.IndexedVector;
import pulse.algebra.Vector;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.linear.LinearOptimiser;
import pulse.tasks.SearchTask;
import pulse.tasks.Status;
import pulse.tasks.TaskManager;
import pulse.util.PropertyHolder;
import pulse.util.Reflexive;

/**
 * An abstract class that defines the mathematical basis of solving the reverse
 * heat conduction problem.
 * <p>
 * Defines the method for calculating the gradient of the target function (the
 * sum of squared residuals, SSR) and a search iteration method, which is used
 * in the main loop of the {@code SearchTask}'s {@code run} method. Declares
 * (but not defines!) the methods for finding the direction of the minimum. This
 * class is closely linked with another abstract search class, the
 * {@code LinearSolver}.
 * </p>
 * 
 * @see pulse.search.tasks.SearchTask.run()
 * @see pulse.search.linear.LinearOptimiser
 */

public abstract class PathOptimiser extends PropertyHolder implements Reflexive {

	private static int maxIterations;
	private static double errorTolerance;
	private static double gradientResolution;

	private static LinearOptimiser linearSolver;
	private static List<Flag> globalSearchFlags = Flag.defaultList();

	private static PathOptimiser selectedPathOptimiser;

	/**
	 * Abstract constructor that sets up the default
	 * {@code ITERATION_LIMIT, ERROR_TOLERANCE} and {@code GRADIENT_RESOLUTION} for
	 * this {@code PathSolver}. In addition, sets up a list of search flags defined
	 * by the {@code Flag.defaultList} method.
	 * 
	 * @see pulse.properties.Flag.defaultList()
	 */

	protected PathOptimiser() {
		super();
		reset();
	}

	/**
	 * Resets the default {@code ITERATION_LIMIT, ERROR_TOLERANCE} and
	 * {@code GRADIENT_RESOLUTION} values for this {@code PathSolver}. In addition,
	 * sets up a list of search flags defined by the {@code Flag.defaultList}
	 * method.
	 * 
	 * @see pulse.properties.Flag.defaultList()
	 */

	public static void reset() {
		maxIterations = (int) NumericProperty.theDefault(ITERATION_LIMIT).getValue();
		errorTolerance = (double) NumericProperty.theDefault(ERROR_TOLERANCE).getValue();
		gradientResolution = (double) NumericProperty.theDefault(GRADIENT_RESOLUTION).getValue();
		globalSearchFlags = Flag.defaultList();
	}

	/**
	 * <p>
	 * This method sets out the basic algorithm for estimating the minimum of the
	 * target function, which is defined as the sum of squared residuals (SSR), or
	 * the deviations of the model solution (a {@code DifferenceScheme} used to
	 * solve the {@code Problem} for this {@code task}) from the empirical values
	 * (the {@code ExperimentalData}). The algorithm will go through the following
	 * steps: (1) find the direction, which points to the minimum, using the
	 * concrete {@code direction} method; (2) estimate the magnitude of the step to
	 * reach the minimum using the {@code LinearSolver}; (3) assign a new set of
	 * parameters to the {@code SearchTask}; (4) calculate the new SSR value.
	 * </p>
	 * </p>
	 * 
	 * @param task a {@code SearchTask} that needs to be driven to a minimum of SSR.
	 * @return the SSR value with the newly found parameters.
	 * @see direction(Path)
	 * @see pulse.search.linear.LinearOptimiser
	 */

	public double iteration(SearchTask task) {
		Path p = task.getPath(); // the previous path of the task

		/*
		 * Checks whether an iteration limit has been already reached
		 */

		if (p.getIteration().compareValues(PathOptimiser.getMaxIterations()) > 0)
			task.setStatus(Status.TIMEOUT);

		IndexedVector parameters = task.searchVector()[0]; // get current search vector

		Vector dir = direction(p); // find the direction to the global minimum

		double step = linearSolver.linearStep(task); // find how big the step needs to be to reach the minimum
		p.setLinearStep(step);

		Vector newParams = parameters.sum(dir.multiply(step)); // this set of parameters supposedly corresponds to the
																// minimum
		task.assign(new IndexedVector(newParams, parameters.getIndices())); // assign new parameters to this task

		endOfStep(task); // compute gradients, Hessians, etc. with new parameters

		p.incrementStep(); // increment the counter of successful steps

		return task.solveProblemAndCalculateDeviation(); // calculate the sum of squared residuals
	}

	/**
	 * Finds the direction of the minimum using the previously calculated values
	 * stored in {@code p}.
	 * 
	 * @param p a {@code Path} object
	 * @return a {@code Vector} pointing to the minimum direction for this
	 *         {@code Path}
	 * @see pulse.problem.statements.Problem.optimisationVector(List<Flag>)
	 */

	public abstract Vector direction(Path p);

	/**
	 * Defines a set of procedures to be run at the end of the search iteration.
	 * 
	 * @param task the {@code SearchTask} undergoing optimisation
	 */

	public abstract void endOfStep(SearchTask task);

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
	 */

	public static Vector gradient(SearchTask task) {

		final IndexedVector params = task.searchVector()[0];
		Vector grad = new Vector(params.dimension());

		Vector newParams, shift;
		double ss1, ss2, dx;

		boolean discreteGradient = params.getIndices().stream().anyMatch(index -> NumericProperty.isDiscreet(index));
		dx = discreteGradient ? 2.0 * task.getScheme().getGrid().getXStep() : 2.0 * gradientResolution;

		for (int i = 0; i < params.dimension(); i++) {
			shift = new Vector(params.dimension());
			shift.set(i, 0.5 * dx);

			newParams = params.sum(shift);
			task.assign(new IndexedVector(newParams, params.getIndices()));
			ss2 = task.solveProblemAndCalculateDeviation();

			newParams = params.subtract(shift);
			task.assign(new IndexedVector(newParams, params.getIndices()));
			ss1 = task.solveProblemAndCalculateDeviation();

			grad.set(i, (ss2 - ss1) / dx);

		}

		task.assign(params);

		return grad;

	}

	public static LinearOptimiser getLinearSolver() {
		return linearSolver;
	}

	/**
	 * Assigns a {@code LinearSolver} to this {@code PathSolver} and sets this
	 * object as its parent.
	 * 
	 * @param linearSearch a {@code LinearSolver}
	 */

	public void setLinearSolver(LinearOptimiser linearSearch) {
		PathOptimiser.linearSolver = linearSearch;
		linearSolver.setParent(this);
		super.parameterListChanged();
	}

	public static NumericProperty getErrorTolerance() {
		return NumericProperty.derive(ERROR_TOLERANCE, errorTolerance);
	}

	public static void setErrorTolerance(NumericProperty errorTolerance) {
		PathOptimiser.errorTolerance = (double) errorTolerance.getValue();
	}

	public static void setGradientResolution(NumericProperty resolution) {
		PathOptimiser.gradientResolution = (double) resolution.getValue();
	}

	public static NumericProperty getGradientResolution() {
		return NumericProperty.derive(GRADIENT_RESOLUTION, gradientResolution);
	}

	public static NumericProperty getMaxIterations() {
		return NumericProperty.derive(ITERATION_LIMIT, maxIterations);
	}

	public static void setMaxIterations(NumericProperty maxIterations) {
		PathOptimiser.maxIterations = (int) maxIterations.getValue();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	public static List<Flag> getSearchFlags() {
		return globalSearchFlags;
	}

	/**
	 * This method has been overriden to account for each individual flag in the
	 * {@code List<Flag>} set out by this class.
	 */

	@Override
	public List<Property> genericProperties() {
		List<Property> original = super.genericProperties();
		original.addAll(globalSearchFlags);
		return original;
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
		list.add(NumericProperty.def(GRADIENT_RESOLUTION));
		list.add(NumericProperty.def(ERROR_TOLERANCE));
		list.add(NumericProperty.def(ITERATION_LIMIT));
		for (Flag property : globalSearchFlags)
			list.add(property);
		return list;
	}

	/**
	 * The accepted types are:
	 * <code> GRADIENT_RESOLUTION, ERROR_TOLERANCE, ITERATION_LIMIT</code>.
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		switch (type) {
		case GRADIENT_RESOLUTION:
			setGradientResolution(property);
			break;
		case ERROR_TOLERANCE:
			setErrorTolerance(property);
			break;
		case ITERATION_LIMIT:
			setMaxIterations(property);
			break;
		default:
			break;
		}
	}

	/**
	 * @return {@code false} for {@code PathSolver}
	 */

	@Override
	public boolean ignoreSiblings() {
		return false;
	}

	/**
	 * Finds what properties are being altered in the search
	 * 
	 * @return a {@code List} of property types represented by
	 *         {@code NumericPropertyKeyword}s
	 */

	public static List<NumericPropertyKeyword> activeParameters() {
		return PathOptimiser.getSearchFlags().stream().filter(flag -> (boolean) flag.getValue())
				.map(flag -> flag.getType()).collect(Collectors.toList());
	}

	/**
	 * Finds a {@code Flag} equivalent to {@code flag} in the {@code originalList}
	 * and substitutes its value with {@code flag.getValue}.
	 * 
	 * @param originalList the list where a flag with a type {@code flag.getType()}
	 *                     can be found
	 * @param flag         the flag which will be set
	 */

	public static void setSearchFlag(List<Flag> originalList, Flag flag) {
		Optional<Flag> optional = originalList.stream().filter(f -> f.getType() == flag.getType()).findFirst();

		if (!optional.isPresent())
			return;

		optional.get().setValue(flag.getValue());
	}

	/**
	 * Creates a new {@code Path} suitable for this {@code PathSolver}
	 * 
	 * @param t the task, the optimisation path of which will be tracked
	 * @return a {@code Path} instance
	 */

	public abstract Path createPath(SearchTask t);

	public static PathOptimiser getSelectedPathOptimiser() {
		return selectedPathOptimiser;
	}

	public static void setSelectedPathOptimiser(PathOptimiser selectedPathOptimiser) {
		PathOptimiser.selectedPathOptimiser = selectedPathOptimiser;
		selectedPathOptimiser.setParent(TaskManager.getInstance());
	}

}