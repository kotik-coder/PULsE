package pulse.search.direction;

import static pulse.properties.NumericProperties.compare;

import java.util.List;

import pulse.math.IndexedVector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.Property;
import pulse.search.linear.LinearOptimiser;
import pulse.search.linear.WolfeOptimiser;
import pulse.tasks.SearchTask;
import pulse.tasks.logs.Status;
import pulse.util.InstanceDescriptor;

public abstract class CompositePathOptimiser extends PathOptimiser {

	private InstanceDescriptor<? extends LinearOptimiser> instanceDescriptor = new InstanceDescriptor<LinearOptimiser>(
			"Linear Optimiser Selector", LinearOptimiser.class);

	private LinearOptimiser linearSolver;

	public CompositePathOptimiser() {
		instanceDescriptor.setSelectedDescriptor(WolfeOptimiser.class.getSimpleName());
		linearSolver = instanceDescriptor.newInstance(LinearOptimiser.class);
		linearSolver.setParent(this);
		instanceDescriptor.addListener(() -> initLinearOptimiser());
	}

	private void initLinearOptimiser() {
		setLinearSolver(instanceDescriptor.newInstance(LinearOptimiser.class));
	}

	public boolean iteration(SearchTask task) throws SolverException {
		var p = task.getPath(); // the previous path of the task

		/*
		 * Checks whether an iteration limit has been already reached
		 */

		if (compare(p.getIteration(), getMaxIterations()) > 0) {
			
			task.setStatus(Status.TIMEOUT);
			
		} else {

			var parameters = task.searchVector()[0];	// current parameters
			var dir = getSolver().direction(p);			// find p[k]

			double step = linearSolver.linearStep(task); // find magnitude of step
			p.setLinearStep(step);

			var candidateParams = parameters.sum(dir.multiply(step)); 					// new set of parameters determined through search
			task.assign(new IndexedVector(candidateParams, parameters.getIndices()));	// assign to this task

			prepare(task);		// update gradients, Hessians, etc. -> for the next step, [k + 1]
			p.incrementStep();	// increment the counter of successful steps

			task.solveProblemAndCalculateCost(); // calculate the sum of squared residuals
			
		}
		
		return true;
		
	}

	public LinearOptimiser getLinearSolver() {
		return linearSolver;
	}

	/**
	 * Assigns a {@code LinearSolver} to this {@code PathSolver} and sets this
	 * object as its parent.
	 * 
	 * @param linearSearch a {@code LinearSolver}
	 */

	public void setLinearSolver(LinearOptimiser linearSearch) {
		this.linearSolver = linearSearch;
		linearSolver.setParent(this);
		super.parameterListChanged();
	}

	public InstanceDescriptor<? extends LinearOptimiser> getLinearOptimiserDescriptor() {
		return instanceDescriptor;
	}

	@Override
	public List<Property> listedTypes() {
		List<Property> list = super.listedTypes();
		list.add(instanceDescriptor);
		return list;
	}

}