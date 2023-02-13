package pulse.problem.statements.model;

public class Nitrogen extends Gas {

    private static final long serialVersionUID = -8593450360265855427L;

    public Nitrogen() {
        super(2, 14);
    }

    @Override
    public double thermalConductivity(double t) {
        return Math.sqrt(t) * (-92.39 / t + 1.647 + 5.255E-4 * t) * 1E-3;
    }

}
