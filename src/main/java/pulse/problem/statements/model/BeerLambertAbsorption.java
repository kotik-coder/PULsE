package pulse.problem.statements.model;

public class BeerLambertAbsorption extends AbsorptionModel {

    private static final long serialVersionUID = -7996852815508481089L;

    public BeerLambertAbsorption() {
        super();
    }

    public BeerLambertAbsorption(AbsorptionModel m) {
        super(m);
    }

    @Override
    public double absorption(SpectralRange range, double y) {
        double a = (double) (this.getAbsorptivity(range).getValue());
        return a * Math.exp(-a * y);
    }

    @Override
    public AbsorptionModel copy() {
        return new BeerLambertAbsorption(this);
    }

}
