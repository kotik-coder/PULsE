package pulse.math;

import static java.lang.Math.abs;
import static java.lang.Math.sqrt;
import static pulse.math.ArithmeticOperations.difference;
import static pulse.math.ArithmeticOperations.differenceSquared;
import static pulse.math.ArithmeticOperations.product;
import static pulse.math.ArithmeticOperations.sum;

import pulse.ui.Messages;

/**
 * <p>
 * This is a general class for {@code Vector} operations useful for
 * optimisation. Note it does not currently include cross or mixed products, as
 * this is not needed in {@code PULsE}.
 * </p>
 */

public class Vector {

	private double[] x;

	public Vector(double[] x) {
		this.x = new double[x.length];
		System.arraycopy(x, 0, this.x, 0, x.length);
	}

	/**
	 * Creates a zero {@code Vector}.
	 * 
	 * @param n the dimension of the {@code Vector}.
	 */

	public Vector(int n) {
		x = new double[n];
	}

	/**
	 * Copy constructor.
	 * 
	 * @param v The vector to be copied
	 */

	public Vector(Vector v) {
		this(v.x);
	}

	/**
	 * Creates a new {@code Vector} based on {@code this} one, all elements of which
	 * are inverted, i.e. <math><i>b</i><sub>i</sub> = -<i>a</i><sub>i</sub></math>.
	 * 
	 * @return a generalised inversion of {@code this Vector}.
	 */

	public Vector inverted() {
		return performOperation(new Vector(dimension()), this, difference);
	}

	/**
	 * The dimension is simply the number of elements in a {@code Vector}
	 * 
	 * @return the integer dimension
	 */

	public int dimension() {
		return x.length;
	}
	
	/**
	 * Performs an element-wise summation of {@code this} and {@code v}.
	 * 
	 * @param v another {@code Vector} with the same number of elements.
	 * @return the result of the summation.
	 */

	public Vector sum(Vector v) {
		return performOperation(this, v, sum);
	}

	/**
	 * Performs an element-wise subtraction of {@code v} from {@code this}.
	 * 
	 * @param v another {@code Vector} with the same number of elements.
	 * @return the result of subtracting {@code v} from {@code this}.
	 * @throws IllegalArgumentException f the dimension of {@code this} and
	 *                                  {@code v} are different.
	 */

	public Vector subtract(Vector v) {
		return performOperation(this, v, difference);
	}

	/**
	 * Performs an element-wise multiplication by {@code f}.
	 * 
	 * @param f a double value.
	 * @return a new {@code Vector}, all elements of which will be multiplied by
	 *         {@code f}.
	 */

	public Vector multiply(double f) {
		Vector factor = new Vector(this);

		for (int i = 0; i < x.length; i++) {
			factor.x[i] *= f;
		}

		return factor;
	}

	/**
	 * Calculates the scalar product of {@code this} and {@code v}.
	 * 
	 * @param v another {@code Vector} with the same dimension.
	 * @return the dot product of {@code this} and {@code v}.
	 */

	public double dot(Vector v) {
		return reduce(this, v, product);
	}

	/**
	 * Calculates the length, which is represented by the square-root of the squared
	 * length.
	 * 
	 * @return the calculated length.
	 * @see lengthSq()
	 */

	public double length() {
		return sqrt(lengthSq());
	}

	/**
	 * The squared length of this vector is the dot product of this vector by
	 * itself.
	 * 
	 * @return the squared length.
	 */

	public double lengthSq() {
		return this.dot(this);
	}

	/**
	 * Performs normalisation, e.g. scalar multiplication of {@code this} by the
	 * multiplicative inverse of {@code this Vector}'s length.
	 * 
	 * @return a normalised {@code Vector} obtained from {@code this}.
	 */

	public Vector normalise() {
		return this.multiply(1.0 / length() );
	}

	/**
	 * Calculates the squared distance from {@code this Vector} to {@code v} (which
	 * is the squared length of the connecting {@code Vector}).
	 * 
	 * @param v another {@code Vector}.
	 * @return the squared length of the connecting {@code Vector}.
	 * @throws IllegalArgumentException f the dimension of {@code this} and
	 *                                  {@code v} are different.
	 */

	public double distanceToSq(Vector v) throws IllegalArgumentException {
		return reduce(this, v, differenceSquared);
	}

	/**
	 * Calculates distance from {@code this Vector} to {@code p} using square-root
	 * on the squared distance.
	 * 
	 * @param p another {@code Vector}.
	 * @return the length of the connecting {@code Vector}.
	 * @see distanceToSq(Vector)
	 * @throws IllegalArgumentException f the dimension of {@code this} and
	 *                                  {@code v} are different.
	 */

	public double distanceTo(Vector p) {
		return sqrt(this.distanceToSq(p));
	}

	/**
	 * Gets the component of {@code this Vector} specified by {@code index}
	 * 
	 * @param index the index of the component
	 * @return a double value, representing the value of the component
	 */

	public double get(int index) {
		return x[index];
	}

	/**
	 * Sets the component of {@code this Vector} specified by {@code index} to
	 * {@code value}.
	 * 
	 * @param index the index of the component.
	 * @param value a new value that will replace the old one.
	 */

	public void set(int index, double value) {
		x[index] = value;
	}

	/**
	 * Defines the string representation of the current instance of the class.
	 * 
	 * @return the string-equivalent of this object containing all it's field
	 *         values.
	 */

	@Override
	public String toString() {
		final String f = Messages.getString("Math.DecimalFormat"); //$NON-NLS-1$
		StringBuilder sb = new StringBuilder().append("("); //$NON-NLS-1$
		for (double c : x) {
			sb.append(String.format(f, c) + " "); //$NON-NLS-1$
		}
		sb.append(")"); //$NON-NLS-1$
		return sb.toString();
	}

	/**
	 * Checks if <code>o</code> is logically equivalent to an instance of this
	 * class.
	 * 
	 * @param o An object to compare with <code>this</code> vector.
	 * @return true if <code>o</code> equals <code>this</code>.
	 */

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof Vector))
			return false;

		Vector v = (Vector) o;

		if (v.x.length != x.length)
			return false;

		for (int i = 0; i < x.length; i++) {
			if (Double.doubleToLongBits(this.x[i]) != Double.doubleToLongBits(v.x[i]))
				return false;
		}

		return true;

	}

	/**
	 * Determines the maximum absolute value of the vector components.
	 * 
	 * @return a component having the maximum absolute value.
	 */

	public double maxAbsComponent() {
		double max = abs(x[0]);
		double abs = 0;
		for (int i = 1; i < x.length; i++) {
			abs = abs(x[i]);
			max = abs > max ? abs : max;
		}
		return max;
	}

	public double[] getData() {
		return x;
	}
	
	private static void checkup(Vector v1, Vector v2) {
		if (v1.x.length != v2.x.length)
			throw new IllegalArgumentException(
					Messages.getString("Vector.DimensionError1") + v1.x.length + " != " + v2.x.length);
	}
	
	private static Vector performOperation(Vector v1, Vector v2, ArithmeticOperation op) {
		checkup(v1, v2);
		
		Vector result = new Vector(v2.x.length);

		for (int i = 0; i < v2.x.length; i++) 
			result.x[i] = op.evaluate(v1.get(i), v2.get(i));

		return result;
	}
	
	private static double reduce(Vector v1, Vector v2, ArithmeticOperation op) {
		checkup(v1, v2);
		
		double result = 0;

		for (int i = 0; i < v2.x.length; i++) 
			result += op.evaluate(v1.get(i), v2.get(i));

		return result;
	}

}