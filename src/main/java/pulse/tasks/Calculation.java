package pulse.tasks;

import static pulse.input.listeners.CurveEventType.TIME_ORIGIN_CHANGED;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.MODEL_WEIGHT;
import static pulse.properties.NumericPropertyKeyword.TIME_LIMIT;
import static pulse.tasks.logs.Status.INCOMPLETE;
import static pulse.util.Reflexive.instantiate;

import java.util.List;
import java.util.stream.Collectors;
import pulse.Response;

import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.math.Segment;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import static pulse.problem.schemes.solvers.SolverException.SolverExceptionType.ILLEGAL_PARAMETERS;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.GeneralTask;
import pulse.search.statistics.BICStatistic;
import pulse.search.statistics.FTest;
import pulse.search.statistics.ModelSelectionCriterion;
import pulse.search.statistics.OptimiserStatistic;
import pulse.tasks.logs.Status;
import pulse.tasks.processing.Result;
import pulse.ui.components.PropertyHolderTable;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyEvent;
import pulse.util.PropertyHolder;

public class Calculation extends PropertyHolder implements Comparable<Calculation>, Response {

    private Status status;
    public final static double RELATIVE_TIME_MARGIN = 1.01;

    private Problem problem;
    private DifferenceScheme scheme;
    private ModelSelectionCriterion rs;
    private OptimiserStatistic os;
    private Result result;

    private static InstanceDescriptor<? extends ModelSelectionCriterion> instanceDescriptor = new InstanceDescriptor<>(
            "Model Selection Criterion", ModelSelectionCriterion.class);

    //BIC as default
    static {
        instanceDescriptor.setSelectedDescriptor(BICStatistic.class.getSimpleName());
    }

    public Calculation(SearchTask t) {
        status = INCOMPLETE;
        this.initOptimiser();
        setParent(t);
        instanceDescriptor.addListener(() -> initModelCriterion());
    }

    /**
     * Creates an orphan Calculation, retaining some properties of the argument
     *
     * @param c another calculation to be archived.
     */
    public Calculation(Calculation c) {
        this.problem = c.problem.copy();
        this.scheme = c.scheme.copy();
        this.rs = c.rs.copy();
        this.os = c.os.copy();
        this.status = c.status;
        if (c.getResult() != null) {
            this.result = new Result(c.getResult());
        }
    }

    public void clear() {
        this.status = INCOMPLETE;
        this.problem = null;
        this.scheme = null;
    }

    /**
     * <p>
     * After setting and adopting the {@code problem} by this
     * {@code SearchTask}, this will attempt to change the parameters of that
     * {@code problem} in accordance with the loaded {@code ExperimentalData}
     * for this {@code SearchTask} (if not null).Later, if any changes to the
     * properties of that {@code Problem} occur and if the source of that event
     * is either the {@code Metadata} or the {@code PropertyHolderTable}, they
     * will be accounted for by altering the parameters of the {@code problem}
     * accordingly -- immediately after the former take place.
     *
     * @param problem a {@code Problem}
     * @param curve
     */
    public void setProblem(Problem problem, ExperimentalData curve) {
        this.problem = problem;
        problem.setParent(this);
        problem.removeHeatingCurveListeners();
        addProblemListeners(problem, curve);
    }

    private void addProblemListeners(Problem problem, ExperimentalData curve) {
        problem.getProperties().addListener((PropertyEvent event) -> {
            var source = event.getSource();

            if (source instanceof Metadata || source instanceof PropertyHolderTable) {

                var property = event.getProperty();
                if (property instanceof NumericProperty && ((NumericProperty) property).isOptimisable()) {
                    return;
                }

                problem.estimateSignalRange(curve);
                problem.getProperties().useTheoreticalEstimates(curve);
            }
        });

        problem.getHeatingCurve().addHeatingCurveListener(dataEvent -> {

            var event = dataEvent.getType();

            if (event == TIME_ORIGIN_CHANGED) {
                var upperLimitUpdated = RELATIVE_TIME_MARGIN * curve.timeLimit()
                        - (double) problem.getHeatingCurve().getTimeShift().getValue();
                scheme.setTimeLimit(derive(TIME_LIMIT, upperLimitUpdated));
            }

        });
    }

    /**
     * Adopts the {@code scheme} by this {@code SearchTask} and updates the time
     * limit of {@code scheme} to match {@code ExperimentalData}.
     *
     * @param scheme the {@code DiffenceScheme}.
     * @param curve
     */
    public void setScheme(DifferenceScheme scheme, ExperimentalData curve) {
        this.scheme = scheme;

        if (problem != null && scheme != null) {
            scheme.setParent(this);

            var upperLimit = RELATIVE_TIME_MARGIN * curve.timeLimit()
                    - (double) problem.getHeatingCurve().getTimeShift().getValue();

            scheme.setTimeLimit(derive(TIME_LIMIT, upperLimit));

        }

    }

    /**
     * This will use the current {@code DifferenceScheme} to solve the
     * {@code Problem} for this {@code Calculation}.
     *
     * @throws SolverException
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void process() throws SolverException {
        var list = problem.getProperties().findMalformedProperties();
        if (!list.isEmpty()) {
            StringBuilder sb = new StringBuilder("Illegal values:");
            list.forEach(np
                    -> sb.append(String.format("%n %-25s", np))
            );
            throw new SolverException(sb.toString(), ILLEGAL_PARAMETERS);
        }
        ((Solver) scheme).solve(problem);
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Attempts to set the status of this calculation to {@code status}.
     *
     * @param status a status
     * @return {@code true} if this attempt is successful, including the case
     * when the status being set is equal to the current status. {@code false}
     * if the current status is one of the following: {@code DONE},
     * {@code EXECUTION_ERROR}, {@code INCOMPLETE}, {@code IN_PROGRES}, AND the
     * {@code status} being set is {@code QUEUED}.
     */
    public boolean setStatus(Status status) {

        boolean changeStatus = true;

        switch (this.status) {
            case QUEUED:
            case IN_PROGRESS:
                switch (status) {
                    case QUEUED:
                    case READY:
                    case INCOMPLETE:
                        changeStatus = false;
                        break;
                    default:
                }
                break;
            case FAILED:
            case EXECUTION_ERROR:
            case INCOMPLETE:
                //if the TaskManager attempts to run this calculation
                changeStatus = status != Status.QUEUED;
                break;
            default:
        }

        if (changeStatus) {
            this.status = status;
        }

        return changeStatus;

    }

    public NumericProperty weight(List<Calculation> all) {
        var result = def(MODEL_WEIGHT);

        boolean condition = all.stream()
                .allMatch(c -> c.getModelSelectionCriterion().getClass().equals(rs.getClass()));

        if (condition) {
            var list = all.stream().map(a -> (ModelSelectionCriterion) a.getModelSelectionCriterion())
                    .collect(Collectors.toList());
            result = rs.weight(list);
        }

        return result;
    }

    public void setModelSelectionCriterion(ModelSelectionCriterion rs) {
        this.rs = rs;
        rs.setParent(this);
        firePropertyChanged(this, instanceDescriptor);
    }

    public ModelSelectionCriterion getModelSelectionCriterion() {
        return rs;
    }

    public void setOptimiserStatistic(OptimiserStatistic os) {
        this.os = os;
        os.setParent(this);
        initModelCriterion();
    }

    @Override
    public OptimiserStatistic getOptimiserStatistic() {
        return os;
    }

    public Problem getProblem() {
        return problem;
    }

    public void initOptimiser() {
        this.setOptimiserStatistic(
                instantiate(OptimiserStatistic.class, OptimiserStatistic.getSelectedOptimiserDescriptor()));
        this.initModelCriterion();
    }

    public void initModelCriterion() {
        setModelSelectionCriterion(instanceDescriptor.newInstance(ModelSelectionCriterion.class, os));
    }

    public DifferenceScheme getScheme() {
        return scheme;
    }

    @Override
    public void set(NumericPropertyKeyword type, NumericProperty property) {
        // intentionally left blank
    }

    /**
     * Checks if this {@code Calculation} is better than {@code a}.
     *
     * @param a another completed calculation
     * @return {@code true} if another calculation hasn't been completed or if
     * this calculation's statistic is lower than statistic of {@code a}.
     */
    public boolean isBetterThan(Calculation a) {
        boolean result = true;

        if (a.getStatus() == Status.DONE) {
            result = compareTo(a) < 0;  //compare statistic

            //do F-test
            Calculation fBest = FTest.test(this, a);
            //if the models are nested and calculations can be compared
            if (fBest != null) {
                //use the F-test result instead
                result = fBest == this;
            }

        }

        return result;
    }

    /**
     * Compares two calculations based on their model selection criteria.
     *
     * @param arg0 another calculation
     * @return the result of comparing the model selection statistics of
     * {@code this} and {@code arg0}.
     */
    @Override
    public int compareTo(Calculation arg0) {
        var sAnother = arg0.getModelSelectionCriterion().getStatistic();
        var sThis = getModelSelectionCriterion().getStatistic();;
        return sThis.compareTo(sAnother);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }

        if (o == null) {
            return false;
        }

        if (!(o instanceof Calculation)) {
            return false;
        }

        var c = (Calculation) o;

        return (os.getStatistic().equals(c.getOptimiserStatistic().getStatistic())
                && rs.getStatistic().equals(c.getModelSelectionCriterion().getStatistic()));

    }

    public static InstanceDescriptor<? extends ModelSelectionCriterion> getModelSelectionDescriptor() {
        return instanceDescriptor;
    }

    public Result getResult() {
        return result;
    }

    public void setResult(Result result) {
        this.result = result;
        if (result != null) {
            result.setParent(this);
        }
    }

    @Override
    public double evaluate(double t) {
        return problem.getHeatingCurve().interpolateSignalAt(t);
    }
    
    @Override
    public Segment accessibleRange() {
        var hc = problem.getHeatingCurve();
        return new Segment(hc.timeAt(0), hc.timeLimit());
    }

   /**
     * This will use the current {@code DifferenceScheme} to solve the
     * {@code Problem} for this {@code SearchTask} and calculate the SSR value
     * showing how well (or bad) the calculated solution describes the
     * {@code ExperimentalData}.
     *
     * @param task
     * @return the value of SSR (sum of squared residuals).
     * @throws pulse.problem.schemes.solvers.SolverException
     */
    
    @Override
    public double objectiveFunction(GeneralTask task) throws SolverException {
        process();
        os.evaluate(task);
        return (double) os.getStatistic().getValue();
    }

}