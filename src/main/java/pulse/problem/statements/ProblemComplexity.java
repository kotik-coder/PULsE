package pulse.problem.statements;

import java.awt.Color;

public enum ProblemComplexity {

    LOW(Color.green), MODERATE(Color.yellow), HIGH(Color.red);

    private Color clr;

    private ProblemComplexity(Color clr) {
        this.clr = clr;
    }

    public Color getColor() {
        return clr;
    }

}
