package pulse.problem.schemes.radiation;

public abstract class Integrator {

	private double cutoff;
	private int tableSize;
	protected int segmentPartitions;
	
	private double values[][];
	
	public Integrator(double cutoff, int numPartitions, int expIntPrecision) {
		this.cutoff = cutoff;
		this.tableSize = numPartitions;
		this.segmentPartitions = expIntPrecision;
	}
	
	/*
	 * Initialises integration table.
	 */
	
	public void init() {
		values = new double[tableSize + 1][4];
		final double H = dx();
		for(int i = 0; i < tableSize; i++) { 
			for(int j = 1; j < 4; j++) 
				values[i][j] = integrate(j, i*H);
		}
	}
	
	/**
	 * Uses linear interpolation to retrieve integral value from pre-calculated table.
	 */
	
	public double integralAt(double t, int n) {
		if(t > cutoff) 
			return 0.0;
		
		final double H = dx();
		
		double _tH = t/H;
		int i = (int) _tH;
		double delta = _tH - i;
		
		return values[i+1][n]*delta + values[i][n]*(1.0 - delta);
	}
	
	/**
	 * t = params[0]
	 * @param params
	 * @return
	 */
	
	public abstract double integrate(int order, double...params);
	
	/**
	 * mu = params[0]
	 * t = params[1]
	 * @param params
	 * @return
	 */
	
	public abstract double integrand(int order, double... params);

	public int getNumPartitions() {
		return tableSize;
	}

	public void setNumPartitions(int numPartitions) {
		this.tableSize = numPartitions;
	}

	public double getCutoff() {
		return cutoff;
	}

	public void setCutoff(double cutoff) {
		this.cutoff = cutoff;
	}

	public int getExpIntPrecision() {
		return segmentPartitions;
	}

	public void setExpIntPrecision(int expIntPrecision) {
		this.segmentPartitions = expIntPrecision;
	}
	
	public double dx() {
		return cutoff/tableSize;
	}
	
}