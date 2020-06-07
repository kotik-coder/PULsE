package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;

public class TrapezoidalScheme extends FixedStepIntegrator {
	
	private double k1;
	
	public double getK1() {
		return k1;
	}

	public TrapezoidalScheme(DiscreteIntensities intensities, EmissionFunction ef, PhaseFunction ipf) {
		super(intensities, ef, ipf);
	}

	public double partialStep(int i, int j, double sign, double gamma) {
		
		double d			 = gamma / 2.0;
		
		double h			 = intensities.grid.step(j, sign);
		final double hSigned = h * sign;
		final double t		 = intensities.grid.getNode(j);
	
		double pf = ipf.function(i, i);
		
		int increment = (int)(1*sign);
		
		k1 = super.rhs( i, j, t, intensities.I[i][j] );
		
		double denominator = intensities.mu[i] + hSigned*d*(1.0 - 0.5*getAlbedo()*pf*intensities.w[i] );
		double numerator = intensities.mu[i]*(intensities.I[i][j] + d*hSigned*k1) + d*hSigned
				*( 0.5*getAlbedo()*ipf.integrateWithoutPoint(i, j + 0*increment, i) + sourceEmission(t + gamma*hSigned) );
		
		return numerator/denominator;
	}
	
	public double step(int i, int j, double sign) {
		return partialStep(i, j, sign, 1.0);
	}
	
	@Override
	public double stepLeft(int i, int j) {
		return step(i, j, -1.0);
	}

	@Override
	public double stepRight(int i, int j) {
		return step(i, j, 1.0);
	}

}