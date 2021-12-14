package pulse.problem.statements.model;

public class BeerLambertAbsorption extends AbsorptionModel {

    @Override
    public double absorption(SpectralRange range, double y) {
        double a = (double) (this.getAbsorptivity(range).getValue());
        return a * Math.exp(-a * y);
    }

}
