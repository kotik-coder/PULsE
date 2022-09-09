package pulse.problem.statements.model;

public class Helium extends Gas {
    
    public Helium() {
        super(1, 4);
    }

    @Override
    public double thermalConductivity(double t) {
        return 0.415 + 0.283E-3 * (t - 1200);
    }
    
}