package pulse.search.direction;

import static pulse.properties.NumericProperties.compare;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperties.isDiscrete;
import static pulse.properties.NumericPropertyKeyword.ERROR_TOLERANCE;
import static pulse.properties.NumericPropertyKeyword.GRADIENT_RESOLUTION;
import static pulse.properties.NumericPropertyKeyword.ITERATION_LIMIT;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.math.IndexedVector;
import pulse.math.linear.Vector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.search.linear.LinearOptimiser;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.logs.Status;
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

	private static PathOptimiser instance;

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
		maxIterations = (int) def(ITERATION_LIMIT).getValue();
		errorTolerance = (double) def(ERROR_TOLERANCE).getValue();
		gradientResolution = (double) def(GRADIENT_RESOLUTION).getValue();
		ActiveFlags.reset();
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
	 * @throws SolverException
	 * @see direction(Path)
	 * @see pulse.search.linear.LinearOptimiser
	 */

	public double iteration(SearchTask task) throws SolverException {
		var p = task.getPath(); // the previous path of the task

		/*
		 * Checks whether an iteration limit has been already reached
		 */

		if (compare(p.getIteration(), getMaxIterations()) > 0)
			task.setStatus(Status.TIMEOUT);

		var parameters = task.searchVector()[0]; // get current search vector

		var dir = direction(p); // find the direction to the global minimum

		double step = linearSolver.linearStep(task); // find how big the step needs to be to reach the minimum
		p.setLinearStep(step);

		var newParams = parameters.sum(dir.multiply(step)); // this set of parameters supposedly corresponds to the
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
	 * @throws SolverException
	 */

	public abstract void endOfStep(SearchTask task) throws SolverException;

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

	public static Vector gradient(SearchTask task) {

		final var params = task.searchVector()[0];
		var grad = new Vector(params.dimension());

		boolean discreteGradient = params.getIndices().stream().anyMatch(index -> isDiscrete(index));
		final double dx = discreteGradient ? 2.0 * task.getScheme().getGrid().getXStep() : 2.0 * gradientResolution;

		for (int i = 0; i < params.dimension(); i++) {
			final var shift = new Vector(params.dimension());
			shift.set(i, 0.5 * dx);

			task.assign(new IndexedVector( params.sum(shift) , params.getIndices()));
			final double ss2 = task.solveProblemAndCalculateDeviation();

			task.assign(new IndexedVector( params.subtract(shift), params.getIndices()));
			final double ss1 = task.solveProblemAndCalculateDeviation();

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
		return derive(ERROR_TOLERANCE, errorTolerance);
	}

	public static void setErrorTolerance(NumericProperty errorTolerance) {
		PathOptimiser.errorTolerance = (double) errorTolerance.getValue();
	}

	public static void setGradientResolution(NumericProperty resolution) {
		PathOptimiser.gradientResolution = (double) resolution.getValue();
	}

	public static NumericProperty getGradientResolution() {
		return derive(GRADIENT_RESOLUTION, gradientResolution);
	}

	public static NumericProperty getMaxIterations() {
		return derive(ITERATION_LIMIT, maxIterations);
	}

	public static void setMaxIterations(NumericProperty maxIterations) {
		PathOptimiser.maxIterations = (int) maxIterations.getValue();
	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

	/**
	 * This method has been overriden to account for each individual flag in the
	 * {@code List<Flag>} set out by this class.
	 */

	@Override
	public List<Property> genericProperties() {
		var original = super.genericProperties();
		original.addAll(ActiveFlags.getProblemDependentFlags());
		original.addAll(ActiveFlags.getProblemIndependentFlags());
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
		List<Property> list = new ArrayList<Property>();
		list.add(def(GRADIENT_RESOLUTION));
		list.add(def(ERROR_TOLERANCE));
		list.add(def(ITERATION_LIMIT));

		ActiveFlags.listAvailableProperties(list);

		return list;
	}

	@Override
	public List<Property> data() {
		var list = listedTypes();
		return super.data().stream().filter(p -> list.contains(p)).collect(Collectors.toList());
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
		return true;
	}


	/**
	 * Finds a {@code Flag} equivalent to {@code flag} in the {@code originalList}
	 * and substitutes its value with {@code flag.getValue}.
	 * 
	 * @param originalList the list where a flag with a type {@code flag.getType()}
	 *                     can be found
	 * @param flag         the flag which will be set
	 */

	public void update(Property property) {
		if(! (property instanceof Flag) )
			super.update(property);
		else {
			var flag = (Flag) property;
			var optional = ActiveFlags.getAllFlags().stream().filter(f -> f.getType() == flag.getType()).findFirst();
			
			if (optional.isPresent())
				optional.get().setValue((boolean) flag.getValue());
			
		}
	}
	
	/**
	 * Creates a new {@code Path} suitable for this {@code PathSolver}
	 * 
	 * @param t the task, the optimisation path of which will be tracked
	 * @return a {@code Path} instance
	 */

	public abstract Path createPath(SearchTask t);

	public static PathOptimiser getInstance() {
		return instance;
	}

	public static void setInstance(PathOptimiser selectedPathOptimiser) {
		PathOptimiser.instance = selectedPathOptimiser;
		selectedPathOptimiser.setParent(TaskManager.getManagerInstance());
	}

}