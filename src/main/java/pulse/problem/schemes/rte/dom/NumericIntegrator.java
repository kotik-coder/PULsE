package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.Grid;
import pulse.problem.schemes.rte.EmissionFunction;
import pulse.problem.statements.ParticipatingMedium;

public abstract class NumericIntegrator {

	protected DiscreteIntensities intensities;
	protected EmissionFunction emissionFunction;
	protected IntegratedPhaseFunction ipf;
	
	private double albedo;
	protected double[] uExtended;

	public NumericIntegrator(DiscreteIntensities intensities, EmissionFunction ef, IntegratedPhaseFunction ipf) {
		this.intensities = intensities;
		this.emissionFunction = ef;
		this.ipf = ipf;
	}	
	
	public void init(ParticipatingMedium problem, Grid grid) {
		setAlbedo( (double)problem.getScatteringAlbedo().getValue() );
		this.emissionFunction.init(problem);
		emissionFunction.setGridStep( grid.getXStep() );
		intensities.setEmissivity((double)problem.getEmissivityProperty().getValue());
	}
	
	public abstract void integrate();
	
	public double rhs(int i, int j, double t, double intensity) {
		return 1.0/intensities.mu[i]*( source(i, j, t) - intensity );
	}
	
	public double source(int i, int j, double t) {
		double tau0 = intensities.grid.getDimension();
		return (1.0 - albedo)*emissionFunction.J(uExtended, t/tau0 ) + 0.5*albedo*ipf.compute(i, j);
	}

	public double getAlbedo() {
		return albedo;
	}

	public void setAlbedo(double albedo) {
		this.albedo = albedo;
	}
	
	public void setTemperatureArray(double[] uExtended) {
		this.uExtended = uExtended;
	}
	
	public double[] getTemperatureArray() {
		return uExtended;
	}

}