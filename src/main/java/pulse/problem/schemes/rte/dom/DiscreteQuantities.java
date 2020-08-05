package pulse.problem.schemes.rte.dom;

/**
 * Defines the main quantities calculated within the discrete ordinates method. This
 * includes the various intensity and flux arrays used internally by the integrators.
 */

class DiscreteQuantities {

	private double qLeft;
	private double qRight;
	
	private double[][] I;
	private double[][] Ik;
	private double[][] f;
	private double[][] fk;
	private double[] qLast;
	
	/**
	 * Constructs a set of quantities based on the specified 
	 * spatial and angular discretisation.
	 * @param gridDensity the DOM grid density
	 * @param ordinates the number of angular nodes
	 */
	
	public DiscreteQuantities(int gridDensity, int ordinates) {
		init(gridDensity, ordinates);
	}
	
	public void init(int gridDensity, int ordinates) {
		I = new double[gridDensity + 1][ordinates];
		f = new double[gridDensity + 1][ordinates];
		Ik = new double[gridDensity + 1][ordinates];
		fk = new double[gridDensity + 1][ordinates];
		qLast = new double[ordinates];
	}
	
	public void store() {
		final int n = I.length;
		final int m = I[0].length;
		
		Ik = new double[n][m];
		fk = new double[n][m];

		/*
		 * store k-th components
		 */
		for (int j = 0; j < Ik.length; j++) {
			System.arraycopy(I[j], 0, Ik[j], 0, Ik[0].length);
			System.arraycopy(f[j], 0, fk[j], 0, fk[0].length);
		}

	}
	
	public double[][] getIntensities() {
		return I;
	}

	protected void clearBoundaryFluxes() {
		qLeft = 0.0;
		qRight = 0.0;
	}
	
	protected double getFluxLeft() {
		return qLeft;
	}
	
	protected void setFluxLeft(double qLeft) {
		this.qLeft = qLeft;
	}

	protected double getFluxRight() {
		return qRight;
	}

	protected void setFluxRight(double qRight) {
		this.qRight = qRight;
	}
	
	public double[][] getDerivatives() {
		return f;
	}
	
	public double getQLast(int i) {
		return qLast[i];
	}

	public void setQLast(int i, double q) {
		this.qLast[i] = q;
	}

	public double getDerivative(int i, int j) {
		return f[i][j];
	}

	public void setDerivative(int i, int j, double f) {
		this.f[i][j] = f;
	}

	public double getStoredIntensity(final int i, final int j) {
		return Ik[i][j];
	}

	public double getStoredDerivative(final int i, final int j) {
		return fk[i][j];
	}

	public void setStoredDerivative(final int i, final int j, final double f) {
		this.f[i][j] = f;
	}
	
	public double getIntensity(int i, int j) {
		return I[i][j];
	}

	public void setIntensity(int i, int j, double value) {
		I[i][j] = value;
	}
	
}