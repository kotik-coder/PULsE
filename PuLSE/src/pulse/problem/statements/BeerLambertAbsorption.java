package pulse.problem.statements;

public class BeerLambertAbsorption extends AbsorptionModel {

	public BeerLambertAbsorption() {
		super();
	}
	
	@Override
	public double absorption(double y) {
		return a0*Math.exp(-a0*y);
	}
	
}