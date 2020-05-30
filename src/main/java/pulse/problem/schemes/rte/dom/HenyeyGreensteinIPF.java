package pulse.problem.schemes.rte.dom;

public class HenyeyGreensteinIPF extends IntegratedPhaseFunction {
	
	public HenyeyGreensteinIPF(DiscreteIntensities intensities) {
		super(intensities);
	}

	@Override
	public double compute(int i, int j) {
		double result = 0;
		double a1 = 1.0 - getAnisotropyFactor()*getAnisotropyFactor();
		double a2 = 1.0 + getAnisotropyFactor()*getAnisotropyFactor();
		double b1 = 2.0*getAnisotropyFactor()*intensities.mu[i];
		for(int k = 0, nHalf = intensities.n/2;
				k < nHalf; k++)
			result += intensities.w[k]*intensities.I[k][j]*a1/( (a2 - b1*intensities.mu[k]) *Math.sqrt(a2 - b1*intensities.mu[k]) ); 
		return result;	
	}

}