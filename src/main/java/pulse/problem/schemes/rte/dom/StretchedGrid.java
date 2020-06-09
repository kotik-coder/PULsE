package pulse.problem.schemes.rte.dom;

public class StretchedGrid {

	private final static double STRETCHING_FACTOR = 3.0;
	public final static int DEFAULT_GRID_DENSITY = 8;
	private double[] nodes;

	private double stretchingFactor;
	private double dimension;

	public void setDimension(double dimension) {
		this.dimension = dimension;
	}

	public StretchedGrid(double dimension) {
		this(DEFAULT_GRID_DENSITY, dimension, STRETCHING_FACTOR);
	}

	public StretchedGrid(int n, double dimension) {
		this(n, dimension, STRETCHING_FACTOR);
	}

	public StretchedGrid(int n, double dimension, double stretchingFactor) {
		this.stretchingFactor = stretchingFactor;
		this.dimension = dimension;
		if (Double.compare(stretchingFactor, 1.0) == 0)
			generateUniform(n, true);
		else
			generate(n);
	}

	public void generate(int n) {
		generateUniform(n, false);

		// apply stretching function
		
		for (int i = 0; i < nodes.length; i++) 
			nodes[i] = 0.5 * dimension * tanh(nodes[i], stretchingFactor);
		
	}

	public void generateUniform(int n, boolean scaled) {
		nodes = new double[n + 1];
		double h = (scaled ? dimension : 1.0) / (n);

		for (int i = 0; i < nodes.length; i++)
			nodes[i] = i * h;

	}

	public int getDensity() {
		return nodes.length - 1;
	}

	public double getDimension() {
		return dimension;
	}

	public double getNode(int i) {
		return nodes[i];
	}

	public double[] getNodes() {
		return nodes;
	}

	public double interpolateStretched(double[] array, double hx, int index) {
		double t = index * hx * dimension;

		// loops through nodes sorted in ascending order
		for (int i = 0; i < nodes.length; i++)
			/*
			 * if node is greater than t, then the associated function can be interpolated
			 * between points f_i and f_i-1, since t lies between nodes n_i and n_i-1
			 */
			if (nodes[i] > t) {
				double mu = (t - nodes[i - 1]) / stepRight(i - 1);
				return array[i] * mu + array[i - 1] * (1.0 - mu);
			}

		// return last element if the condition has still not been satisfied
		return array[nodes.length - 1];

	}

	/**
	 * Assumes uniform grid
	 * 
	 * @param array
	 * @param hx
	 * @param index
	 * @return
	 */

	public double interpolateUniform(double[] array, double hx, int index) {
		double t = index * hx * dimension;
		double h = nodes[1] - nodes[0];
		int floor = (int) (t / h);
		double alpha = t - floor * h;
		return (1.0 - alpha) * array[floor] + alpha * array[floor + 1];
	}

	public double step(int i, double sign) {
		return nodes[i + (int) ((1. + sign) * 0.5)] - nodes[i - (int) ((1. - sign) * 0.5)];
	}

	public double stepLeft(int i) {
		return nodes[i] - nodes[i - 1];
	}

	public double stepRight(int i) {
		return nodes[i + 1] - nodes[i];
	}

	public double tanh(final double x, final double stretchingFactor) {
		return 1.0 - Math.tanh(stretchingFactor * (1.0 - 2.0*x)) / Math.tanh(stretchingFactor);
	}

}