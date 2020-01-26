package pulse.problem.statements;

public class BeerLambertAbsorption extends AbsorptionModel {

	public BeerLambertAbsorption(SpectralRange spectrum) {
		super(spectrum);
	}
	
	@Override
	public double absorption(double y) {
		return a0*Math.exp(-a0*y);
	}
	
}