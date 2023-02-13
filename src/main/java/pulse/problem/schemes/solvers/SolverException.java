package pulse.problem.schemes.solvers;

@SuppressWarnings("serial")
public class SolverException extends Exception {

    private final SolverExceptionType type;

    public SolverException(String status, SolverExceptionType type) {
        super(status);
        this.type = type;
    }

    public SolverException(SolverExceptionType type) {
        this(type.toString(), type);
    }

    public SolverExceptionType getType() {
        return type;
    }

    public enum SolverExceptionType {
        RTE_SOLVER_ERROR,
        OPTIMISATION_ERROR,
        OPTIMISATION_TIMEOUT,
        FINITE_DIFFERENCE_ERROR,
        ILLEGAL_PARAMETERS,
    }

}
