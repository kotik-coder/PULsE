package pulse.search.direction;

import static pulse.properties.NumericProperties.compare;

import java.util.List;

import pulse.math.ParameterVector;
import static pulse.math.linear.Matrices.createIdentityMatrix;
import pulse.problem.schemes.solvers.SolverException;
import static pulse.problem.schemes.solvers.SolverException.SolverExceptionType.OPTIMISATION_TIMEOUT;
import pulse.properties.Property;
import pulse.search.GeneralTask;
import pulse.search.linear.LinearOptimiser;
import pulse.search.linear.WolfeOptimiser;
import pulse.util.InstanceDescriptor;

public abstract class CompositePathOptimiser extends GradientBasedOptimiser {

    private InstanceDescriptor<? extends LinearOptimiser> instanceDescriptor 
            = new InstanceDescriptor<>(
            "Linear Optimiser Selector", LinearOptimiser.class);

    private LinearOptimiser linearSolver;
    
     /**
     * Maximum number of consequent failed iterations that can be rejected.
     * Up to {@value MAX_FAILED_ATTEMPTS} failed attempts are allowed.
     */
    
    public final static int MAX_FAILED_ATTEMPTS = 2;
    
    /**
     * For numerical comparison.
     */
    public final static double EPS = 1e-10; 

    public CompositePathOptimiser() {
        instanceDescriptor.setSelectedDescriptor(WolfeOptimiser.class.getSimpleName());
        linearSolver = instanceDescriptor.newInstance(LinearOptimiser.class);
        linearSolver.setParent(this);
        instanceDescriptor.addListener(() -> initLinearOptimiser());
    }

    private void initLinearOptimiser() {
        setLinearSolver(instanceDescriptor.newInstance(LinearOptimiser.class));
    }

    @Override
    public boolean iteration(GeneralTask task) throws SolverException {
        var p = (GradientGuidedPath) task.getIterativeState(); // the previous state of the task
        
        boolean accept = true;

        /*
		 * Checks whether an iteration limit has been already reached
         */
        if (compare(p.getIteration(), getMaxIterations()) > 0) {

            throw new SolverException(OPTIMISATION_TIMEOUT);

        } else {

            double initialCost = task.getResponse().objectiveFunction(task);
            p.setCost(initialCost);
            var parameters     = task.searchVector();  

            p.setParameters(parameters); // store current parameters

            var dir = getSolver().direction(p);		// find p[k]
            double step = linearSolver.linearStep(task); // find magnitude of step
            p.setLinearStep(step);

            // new set of parameters determined through search
            var candidateParams = parameters.toVector().sum(dir.multiply(step)); 		
            var candidateVector = new ParameterVector(parameters, candidateParams);
            
            if(candidateVector.findMalformedElements().isEmpty()) {
                task.assign(candidateVector); // assign new parameters
            }
            
            double newCost = task.getResponse().objectiveFunction(task); 
            // calculate the sum of squared residuals

            if (newCost > initialCost - EPS 
                    && p.getFailedAttempts() < MAX_FAILED_ATTEMPTS 
                    && p instanceof ComplexPath) {  
                var complexPath = (ComplexPath)p;
                task.assign(parameters);    // roll back if cost increased             
                // attempt to reset -> in case of Hessian-based methods, 
                // this will change the Hessian) {
                complexPath.setHessian( createIdentityMatrix(parameters.dimension()) );                
                p.incrementFailedAttempts();
                accept = false;
            } else {
                task.storeState();
                p.resetFailedAttempts();
                this.prepare(task);	 // update gradients, Hessians, etc. -> for the next step, [k + 1]
                p.setCost(newCost);
                p.incrementStep();       // increment the counter of successful steps
            }                                                                                                          

        }

        return accept;

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

    /**
     * Creates a new {@code Path} instance for storing the gradient, direction,
     * and minimum point for this {@code PathSolver}.
     *
     * @param t the search task
     * @return a {@code Path} instance
     */
    @Override
    public GradientGuidedPath initState(GeneralTask t) {
        return new ComplexPath(t);
    }

}