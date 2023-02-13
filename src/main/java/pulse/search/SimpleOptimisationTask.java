package pulse.search;

import java.util.List;
import java.util.stream.Collectors;
import pulse.DiscreteInput;
import pulse.math.ParameterIdentifier;
import pulse.math.ParameterVector;
import pulse.problem.schemes.solvers.SolverException;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.ActiveFlags;
import static pulse.search.direction.ActiveFlags.selectActiveAndListed;
import pulse.search.direction.LMOptimiser;
import pulse.search.direction.PathOptimiser;
import pulse.util.PropertyHolder;

/**
 * Generic optimisation class.
 *
 * @param <T> an optimisable object
 */
public abstract class SimpleOptimisationTask<T extends PropertyHolder & Optimisable>
        extends GeneralTask {

    private final T optimisable;
    private final DiscreteInput input;

    public SimpleOptimisationTask(T optimisable, DiscreteInput input) {
        this.input = input;
        this.optimisable = optimisable;
    }

    @Override
    public void run() {
        var optimiser = PathOptimiser.getInstance();
        if (optimiser == null) {
            PathOptimiser.setInstance(LMOptimiser.getInstance());
        }
        super.run();
    }

    /**
     * Generates a search vector (= optimisation vector) using the search flags
     * set by the {@code PathSolver}.
     *
     * @return an {@code IndexedVector} with search parameters of this
     * {@code SearchTaks}
     * @see pulse.search.direction.PathSolver.getSearchFlags()
     * @see pulse.problem.statements.Problem.optimisationVector(List<Flag>)
     */
    @Override
    public ParameterVector searchVector() {
        var ids = activeParameters().stream().map(id
                -> new ParameterIdentifier(id)).collect(Collectors.toList());
        var optimisationVector = new ParameterVector(ids);

        optimisable.optimisationVector(optimisationVector);

        return optimisationVector;
    }

    @Override
    public void assign(ParameterVector pv) throws SolverException {
        optimisable.assign(pv);
    }

    @Override
    public boolean isInProgress() {
        return false;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        optimisable.set(type, property);
    }

    @Override
    public List<NumericPropertyKeyword> activeParameters() {
        return selectActiveAndListed(ActiveFlags.getAllFlags(), optimisable);
    }

    @Override
    public void setDefaultOptimiser() {
        setOptimiser(LMOptimiser.getInstance());
    }

    @Override
    public DiscreteInput getInput() {
        return input;
    }

}
