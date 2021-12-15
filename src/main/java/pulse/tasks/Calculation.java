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

import pulse.input.ExperimentalData;
import pulse.input.Metadata;
import pulse.problem.schemes.DifferenceScheme;
import pulse.problem.schemes.solvers.Solver;
import pulse.problem.schemes.solvers.SolverException;
import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.statistics.AICStatistic;
import pulse.search.statistics.ModelSelectionCriterion;
import pulse.search.statistics.OptimiserStatistic;
import pulse.tasks.logs.Status;
import pulse.tasks.processing.Result;
import pulse.ui.components.PropertyHolderTable;
import pulse.util.InstanceDescriptor;
import pulse.util.PropertyEvent;
import pulse.util.PropertyHolder;

public class Calculation extends PropertyHolder implements Comparable<Calculation> {

    private Status status;
    public final static double RELATIVE_TIME_MARGIN = 1.01;

    private Problem problem;
    private DifferenceScheme scheme;
    private ModelSelectionCriterion rs;
    private OptimiserStatistic os;
    private Result result;

    private static InstanceDescriptor<? extends ModelSelectionCriterion> instanceDescriptor = new InstanceDescriptor<>(
            "Model Selection Criterion", ModelSelectionCriterion.class);

    static {
        instanceDescriptor.setSelectedDescriptor(AICStatistic.class.getSimpleName());
    }

    public Calculation() {
        status = INCOMPLETE;
        this.initOptimiser();
        instanceDescriptor.addListener(() -> initModelCriterion());
    }

    public Calculation(Problem problem, DifferenceScheme scheme, ModelSelectionCriterion rs) {
        this();
        this.problem = problem;
        this.scheme = scheme;
        this.os = rs.getOptimiser();
        this.rs = rs;
        problem.setParent(this);
        scheme.setParent(this);
        os.setParent(this);
        rs.setParent(this);
    }

    public Calculation copy() {
        var status = this.status;
        var nCalc = new Calculation(problem.copy(), scheme.copy(), rs.copy());
        var p = nCalc.getProblem();
        p.getProperties().setMaximumTemperature(problem.getProperties().getMaximumTemperature());
        nCalc.status = status;
        if (this.getResult() != null) {
            nCalc.setResult(new Result(this.getResult()));
        }
        return nCalc;
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
        problem.retrieveData(curve);
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
     * limit of {
     *
     * @scheme} to match {@code ExperimentalData}.
     *
     * @param scheme the {@code DiffenceScheme}.
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
        ((Solver) scheme).solve(problem);
    }

    public Status getStatus() {
        return status;
    }

    /**
     * Attempts to set the status of this calculation to {@code status}.
     * @param status a status
     * @return {@code true} if this attempt is successful, including the case 
     * when the status being set is equal to the current status. {@code false}
     * if the current status is one of the following: {@code DONE}, {@code EXECUTION_ERROR},
     * {@code INCOMPLETE}, {@code IN_PROGRES}, AND the {@code status} being set 
     * is {@code QUEUED}.
     */
    
    public boolean setStatus(Status status) {

        switch(this.status) {
            case DONE:
            case IN_PROGRESS:
            case FAILED:
            case EXECUTION_ERROR:
            case INCOMPLETE:
                 //if the TaskManager attempts to run this calculation
                if(status == Status.QUEUED) 
                    return false;
            default:
        }
        
        this.status = status;
        return true;
        
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

    @Override
    public int compareTo(Calculation arg0) {
        var s1 = arg0.getModelSelectionCriterion().getStatistic();
        return getModelSelectionCriterion().getStatistic().compareTo(s1);
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

}
