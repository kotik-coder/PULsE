package pulse.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import pulse.DiscreteInput;
import pulse.Response;
import pulse.math.ParameterVector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.IterativeState;
import pulse.search.direction.PathOptimiser;
import pulse.tasks.processing.Buffer;
import static pulse.tasks.processing.Buffer.getSize;
import pulse.util.Accessible;

public abstract class GeneralTask<I extends DiscreteInput, R extends Response> 
        extends Accessible implements Runnable {

    private IterativeState path;    //current sate
    private IterativeState best;    //best state 
    
    private final Buffer buffer;
    private PathOptimiser optimiser;
    
    public GeneralTask() {
        buffer = new Buffer();
        buffer.setParent(this);
    }
    
    public abstract List<NumericPropertyKeyword> activeParameters();

    /**
     * Creates a search vector populated by parameters that
     * are included in the optimisation routine.
     * @return the parameter vector with optimisation parameters
     */
    
    public abstract ParameterVector searchVector();
    
    /**
     * Tries to assign a selected set of parameters to the search vector
     * used in optimisation.
     * @param pv a parameter vector containing all of the optimisation parameters
     * whose values will be assigned to this task
     * @throws SolverException 
     */
    
    public abstract void assign(ParameterVector pv) throws SolverException;
    
     /**
     * <p>
     * Runs this task if is either {@code READY} or {@code QUEUED}. Otherwise,
     * will do nothing. After making some preparatory steps, will initiate a
     * loop with successive calls to {@code PathSolver.iteration(this)}, filling
     * the buffer and notifying any data change listeners in parallel. This loop
     * will go on until either converging results are obtained, or a timeout is
     * reached, or if an execution error happens. Whether the run has been
     * successful will be determined by comparing the associated
     * <i>R</i><sup>2</sup> value with the {@code SUCCESS_CUTOFF}.
     * </p>
     */
    @Override
    public void run() {
        setDefaultOptimiser();
        setIterativeState( optimiser.initState(this) );

        var errorTolerance = (double) optimiser.getErrorTolerance().getValue();
        int bufferSize = (Integer) getSize().getValue();
        buffer.init();
        //correlationBuffer.clear();

        /* search cycle */
        /* sets an independent thread for manipulating the buffer */
        List<CompletableFuture<Void>> bufferFutures = new ArrayList<>(bufferSize);
        var singleThreadExecutor = Executors.newSingleThreadExecutor();

        var response = getResponse();
        
        try {
            response.objectiveFunction(this);
        } catch (SolverException e1) {
            onSolverException(e1);
        }

        outer:
        do {

            bufferFutures.clear();

            for (var i = 0; i < bufferSize; i++) {

                try {
                    for (boolean finished = false; !finished;) {
                        finished = optimiser.iteration(this);
                    }
                } catch (SolverException e) {
                    onSolverException(e);
                    break outer;
                }
                
                //if global best is better than the converged value
                if (best != null && best.getCost() < path.getCost()) {
                    try {
                        //assign the global best parameters
                        assign(path.getParameters());
                        //and try to re-calculate
                        response.objectiveFunction(this);
                    } catch (SolverException ex) {
                        onSolverException(ex);
                    }
                }

                final var j = i;
                
                bufferFutures.add(CompletableFuture.runAsync(() -> {
                    buffer.fill(this, j);
                    intermediateProcessing();
                }, singleThreadExecutor));

            }

            bufferFutures.forEach(future -> future.join());
        
        } while (buffer.isErrorTooHigh(errorTolerance) 
                && isInProgress());

        singleThreadExecutor.shutdown();

        if (isInProgress()) {
            postProcessing();
        } 

    }        

    public abstract boolean isInProgress();       
    
    /**
     * Override this to add intermediate processing of results e.g.
     * with a correlation test.
     */
    
    public void intermediateProcessing() {
        //empty
    }
    
    /**
     * Specifies what should be done when a solver exception is encountered.
     * Empty by default
     * @param e1 a solver exception
     */
    
    public void onSolverException(SolverException e1) {
        //empty
    }
    
    /**
     * Override this to add post-processing checks
     * e.g. normality tests or range checking.
     */
    
    public void postProcessing() {
        //empty
    }
    
    public final Buffer getBuffer() {
        return buffer;
    }
    
    public void setIterativeState(IterativeState state) {
        this.path = state;
    }

    public IterativeState getIterativeState() {
        return path;
    }

    public IterativeState getBestState() {
        return best;
    }

    /**
     * Update the best state. The instance of this class stores two objects of
     * the type IterativeState: the current state of the optimiser and the
     * global best state. Calling this method will check if a new global best is
     * found, and if so, this will store its parameters in the corresponding
     * variable. This will then be used at the final stage of running the search
     * task, comparing the converged result to the global best, and selecting
     * whichever has the lowest cost. Such routine is required due to the
     * possibility of some optimisers going uphill.
     */
    public void storeState() {
        if (best == null || best.getCost() > path.getCost()) {
            best = new IterativeState(path);
        }
    }
    
    public final void setOptimiser(PathOptimiser optimiser) {
        this.optimiser = optimiser;
    }
    
    public void setDefaultOptimiser() {
        var instance = PathOptimiser.getInstance();
        if(optimiser == null || optimiser != instance) {
            setOptimiser(PathOptimiser.getInstance());
        }
    }
    
    public double objectiveFunction() throws SolverException {
        return getResponse().objectiveFunction(this);
    }
    
    public abstract I getInput();    
    public abstract R getResponse();
    
}