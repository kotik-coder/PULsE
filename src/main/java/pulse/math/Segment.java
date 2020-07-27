package pulse.math;

import java.util.Random;

/**
 * A {@code Segment} is simply a pair of values {@code a} and {@code b} such
 * that {@code a < b}.
 *
 */

public class Segment {
	private double a;
	private double b;
	
	/**
	 * Creates a {@code Segment} bounded by {@code a} and {@code b}.
	 * 
	 * @param a any value
	 * @param b either {@code b > a} or {@code b < a}
	 */

	public Segment(double a, double b) {
		this.a = a < b ? a : b;
		this.b = b > a ? b : a;
	}

	/**
	 * Copies {@code segment}
	 * 
	 * @param segment a {@code Segment}
	 */

	public Segment(Segment segment) {
		this.a = segment.a;
		this.b = segment.b;
	}

	/**
	 * Gets the {@code a} value for this {@code Segment}
	 * 
	 * @return the lower end of this {@code Segment}
	 */

	public double getMinimum() {
		return a;
	}

	/**
	 * Gets the {@code b} value for this {@code Segment}
	 * 
	 * @return the upper end of this {@code Segment}
	 */

	public double getMaximum() {
		return b;
	}

	/**
	 * Calculates the length {@code (b - a)}
	 * 
	 * @return the length value
	 */

	public double length() {
		return (b - a);
	}

	/**
	 * Calculates the squared length
	 * 
	 * @return the squared length value
	 * @see length()
	 */

	public double lengthSq() {
		return Math.pow(b - a, 2);
	}

	/**
	 * Sets the minimum value to {@code a}. Note it does not prevent against:
	 * {@code a >= max}.
	 * 
	 * @param a a value, which should satisfy {@code a < max}
	 */

	public void setMinimum(double a) {
		this.a = a;
	}

	/**
	 * Sets the maximum value to {@code b}. Note it does not prevent against:
	 * {@code b <= min}.
	 * 
	 * @param b a value, which should satisfy {@code b > min}
	 */

	public void setMaximum(double b) {
		this.b = b;
	}

	/**
	 * Calculates the middle point of this {@code Segment}.
	 * 
	 * @return the mean
	 */

	public double mean() {
		return (a + b) * 0.5;
	}

	/**
	 * Calculates a random value confined in the interval between the two ends of
	 * this {@code Segment}.
	 * 
	 * @return a confined random value.
	 */

	public double randomValue() {
		return (new Random()).nextDouble() * length() + getMinimum();
	}

	/**
	 * Checks whether {@code x} is contained inside this {@code Segment}.
	 * 
	 * @param x a value.
	 * @return {@code true} if <math>min &leq; <i>x</i> &leq; max</math>.
	 */

	public boolean contains(double x) {
		return x >= a ? (x <= b ? true : false) : false;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		sb.append(a);
		sb.append(" ; ");
		sb.append(b);
		sb.append("]");
		return sb.toString();
	}

}