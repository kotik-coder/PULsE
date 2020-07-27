package pulse.math;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;

/**
 * An abstract class for univariate functions with the capacity of spline interpolation. 
 *
 */

public abstract class FunctionWithInterpolation {

	private Segment tBounds;
	private int lookupTableSize;
	private UnivariateFunction interpolation;
	
	public final static int NUM_PARTITIONS = 8192;
	
	/**
	 * Constructs a {@code FunctionWithInterpolation} by tabulating the function values
	 * within the {@code parameterBounds} at discrete nodes, the number of which is given
	 * by the second argument. After having done this, creates a {@code SplineInterpolation},
	 * which can be invoked in future to calculate intermediate function values without loss
	 * of accuracy.
	 * @param parameterBounds the calculation bounds.
	 * @param lookupTableSize a size of the table for discrete function calculations.
	 */
	
	public FunctionWithInterpolation(Segment parameterBounds, int lookupTableSize) {
		this.tBounds = parameterBounds;
		this.lookupTableSize = lookupTableSize;
		init();
	}
	
	/**
	 * Creates a {@code FunctionWithInterpolation} using the {@code parameterBounds} and 
	 * a default number of points {@value NUM_PARTITIONS}.
	 * @param parameterBounds the calculation bounds. 
	 */
	
	public FunctionWithInterpolation(Segment parameterBounds) {
		this(parameterBounds, NUM_PARTITIONS);
	}
	
	/**
	 * Performs the calculation at {@code t}.
	 * @param t the value of the independent variable
	 * @return the function value at {@code t}
	 */
	
	public abstract double evaluate(double t);

	/**
	 * Uses the stored interpolation function to calculate values at any {@code t} within the parameter bounds.
	 * <b>Note</b>: If {@code t} is not contained within the parameter bounds, the method will return 0.0.
	 * 
	 * @param t the value of the independent variable 
	 * @return will return the interpolated value or 0.0
	 */

	public double valueAt(double t) {
		return tBounds.contains(t) ? interpolation.value(t) : 0.0;
	}
	
	/**
	 * Retrieves the parameter bounds.
	 * @return the parameter bounds.
	 */

	public Segment getParameterBounds() {
		return tBounds;
	}
	
	/**
	 * Sets the parameter bounds to {@code parameterBounds}. The interpolation will then be re-calculated.
	 * @param parameterBounds the new parameter bounds.
	 */

	public void setParameterBounds(Segment parameterBounds) {
		this.tBounds = parameterBounds;
		init();
	}
	
	private void init() {
		var lookupTable = generateTable();
		interpolate(lookupTable);
	}

	private double[] generateTable() {
		var lookupTable = new double[lookupTableSize];
		final double delta = tBounds.length()/(lookupTableSize - 1);
		
		double t = tBounds.getMinimum();
		for (int i = 0; i < lookupTableSize; i++) { 
			lookupTable[i] = evaluate(t);
			t += delta;
		}
		
		return lookupTable;
	}

	private void interpolate(double[] lookupTable) {
		var tArray = new double[lookupTableSize];
		final double delta = tBounds.length()/(lookupTableSize - 1);
	
		for (int i = 0; i < tArray.length; i++)
			tArray[i] = tBounds.getMinimum() + i * delta;

		var splineInterpolation = new SplineInterpolator();
		interpolation = splineInterpolation.interpolate(tArray, lookupTable);
	}

}