package pulse.ui.components.listeners;

import pulse.problem.statements.Problem;

public class ProblemSelectionEvent {

    private Problem problem;
    private Object source;

    public ProblemSelectionEvent(Problem problem, Object source) {
        this.problem = problem;
        this.source = source;
    }

    public Problem getProblem() {
        return problem;
    }

    public void setProblem(Problem problem) {
        this.problem = problem;
    }

    public Object getSource() {
        return source;
    }

    public void setSource(Object source) {
        this.source = source;
    }

}
