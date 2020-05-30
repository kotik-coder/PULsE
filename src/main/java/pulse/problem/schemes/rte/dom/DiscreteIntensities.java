package pulse.problem.schemes.rte.dom;

import pulse.problem.schemes.rte.EmissionFunction;

public class DiscreteIntensities {

	protected double[][] I;
	protected double[] mu;
	protected double[] w;
	
	protected double[] localFlux, localFluxDerivative;
	protected double[] storedF, storedFD;
	
	protected int n; 
	protected StretchedGrid grid;
	
	private double emissivity;
	private double boundaryFluxFactor;
	private double qLeft, qRight;

	private final static double DOUBLE_PI	= 2.0*Math.PI;
	
	public DiscreteIntensities(double opticalThickness) {
		this.mu = S8QuadratureSet.getNodes();
		this.w = S8QuadratureSet.getWeights();
		
		this.n = mu.length;
		this.grid = new StretchedGrid(opticalThickness);

		this.reinitInternalArrays();
	}
	
	public void fluxes() {
		int N = grid.getDensity();
		//calculate fluxes on DOM grid
		for(int i = 0; i < N + 1; i++)
			localFlux[i] = DOUBLE_PI*q(i);

		localFluxDerivative[0] = (localFlux[0] - localFlux[1])/grid.stepRight(0);
		for(int i = 1; i < N; i++)		
			localFluxDerivative[i] = ( localFlux[i - 1] - localFlux [i + 1] )/( grid.stepRight(i-1) + grid.stepLeft(i + 1) );
		localFluxDerivative[N] = (localFlux[N - 1] - localFlux[N]) / grid.stepLeft(N);
	}

	public void store() {
		System.arraycopy(localFlux, 0, storedF, 0, storedF.length);
		System.arraycopy(localFluxDerivative, 0, storedFD, 0, storedFD.length);
	}
	
	public void clear() {
		for(int i = 0, j = 0, N = grid.getDensity(); i < n; i++)
			for(j = 0; j < N + 1; j++) 
				I[i][j] = 0;
		qLeft	= 0.0;
		qRight	= 0.0;
	}
	
	/**
	 * Calculates the reflected intensity (positive angles, first half of indices)
	 * at the left boundary (tau = 0).
	 */
	
	public void left(double[] uExtended, EmissionFunction ef) {

		for(int i = 0; i < n/2; i++) 		//for positive streams
			I[i][0] = ef.radiance(uExtended[0]) - boundaryFluxFactor*qLeft(ef, uExtended);
	
	}
	
	/**
	 * Calculates the reflected intensity (negative angles, second half of indices)
	 * at the right boundary (tau = tau_0).
	 */
	
	public void right(double[] uExtended, EmissionFunction ef) {		

		for(int i = n/2, N = grid.getDensity(), nT = uExtended.length - 2; 
				i < n; i++) 		//for negative streams
			I[i][N] = ef.radiance(uExtended[nT]) + boundaryFluxFactor*qRight(ef, uExtended);	
			
	}
	
	public double q(int j, int startInclusive, int endExclusive) {
		double integral = 0;
		
		for(int i = startInclusive; i < endExclusive; i++)
			integral += w[i]*I[i][j]*mu[i];
		
		return integral;
		
	}
	
	public double q(int j) {
		double integral = 0;
		
		for(int i = 0, half = n/2; i < half; i++)
			integral += w[i]*(I[i][j] - I[i + half][j])*mu[i];
		
		return integral;
		
	}
	
	/**
	 * Calculates the net zeroth moment of intensity (i.e., the incident radiation) 
	 * for the positive hemisphere).
	 * @param j index on grid
	 * @return incident radiation (positive hemisphere)
	 */
	
	public double g(int j, int startInclusive, int endExclusive) {
		double integral = 0;
		
		for(int i = startInclusive; i < endExclusive; i++)
			integral += w[i]*I[i][j];
		
		return integral;
		
	}
	
	public double g(int j) {
		double integral = 0;
		
		for(int i = 0, half = n/2; i < half; i++)
			integral += w[i]*(I[i][j] + I[i + half][j]);
		
		return integral;
		
	}
	
	private double qLeft(EmissionFunction emissionFunction, double[] uExtended) {
		return qLeft = emissivity*(emissionFunction.power(uExtended[0]) + DOUBLE_PI*q(0, n/2, n));
	}
	
	private double qRight(EmissionFunction emissionFunction, double[] uExtended) {
		return qRight = -emissivity*(emissionFunction.power(uExtended[uExtended.length - 2]) - DOUBLE_PI*q(grid.getDensity(), 0, n/2) );
	}

	public double[][] getIntensities() {
		return I;
	}
	
	public double getQLeft() {
		return qLeft;
	}

	public double getQRight() {
		return qRight;
	}
	
	public double getEmissivity() { 
		return emissivity;
	}
	
	public void setEmissivity(double emissivity) { 
		this.emissivity = emissivity;
		boundaryFluxFactor = (1.0 - emissivity)/(emissivity*Math.PI);
	}

	public void setOpticalThickness(double tau0) {
		this.grid = new StretchedGrid(tau0);
		this.I = new double[n][grid.getDensity() + 1];
	}
	
	public void reinitInternalArrays() {
		int N	= grid.getDensity() + 1;
		I		= new double[n][N];
		localFlux			= new double[N];
		localFluxDerivative = new double[N];
		storedF				= new double[N];
		storedFD			= new double[N];
	}
	
//	public NumericProperty getQuadraturePoints() {
//		return NumericProperty.derive(NumericPropertyKeyword.DOM_DIRECTIONS, n);
//	}
//	
//	public void setQuadraturePoints(NumericProperty m) {
//		if(m.getType() != NumericPropertyKeyword.DOM_DIRECTIONS)
//			throw new IllegalArgumentException("Illegal type: " + m.getType());
//		this.n = (int)m.getValue();
//		
//		discreteDirections = new GaussianQuadrature(n);
//		discreteDirections.init();
//		discreteDirections.setParent(this);
//		
//		mu = S8QuadratureSet.getNodes();
//		w = S8QuadratureSet.getWeights();
//		
//		I = new double[n][this.getExternalGridDensity() + 1];
//		
//	}

}