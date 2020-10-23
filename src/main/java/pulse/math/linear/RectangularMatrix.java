package pulse.math.linear;

import static pulse.math.MathUtils.approximatelyEquals;
import static pulse.math.linear.ArithmeticOperations.DIFFERENCE;
import static pulse.math.linear.ArithmeticOperations.SUM;
import static pulse.math.linear.Matrices.createMatrix;

import pulse.ui.Messages;

public class RectangularMatrix {

	protected final double[][] x;

	protected RectangularMatrix(double[][] args) {
		int m = args.length;
		int n = args[0].length;

		x = new double[m][n];

		for (int i = 0; i < m; i++)
			System.arraycopy(args[i], 0, x[i], 0, n);

	}

	protected RectangularMatrix(double[] data, int n) {
		final int m = data.length / n;
		x = new double[m][n];

		for (int i = 0; i < m; i++)
			System.arraycopy(data, i * n, x[i], 0, n);

	}

	/**
	 * Performs an element-wise summation if {@code this} and {@code m} have
	 * matching dimensions.
	 * 
	 * @param m another {@code Matrix} of the same size as {@code this} one
	 * @return the result of summation
	 */

	public RectangularMatrix sum(RectangularMatrix m) {
		return performOperation(this, m, SUM);
	}

	/**
	 * Performs an element-wise subtraction of {@code m} from {@code this} if these
	 * matrices have matching dimensions.
	 * 
	 * @param m another {@code Matrix} of the same size as {@code this} one
	 * @return the result of subtraction
	 */

	public RectangularMatrix subtract(RectangularMatrix m) {
		return performOperation(this, m, DIFFERENCE);
	}

	/**
	 * <p>
	 * Performs {@code Matrix} multiplication. Checks whether the dimensions of each
	 * matrix are appropriate (number of columns in {@code this} matrix should be
	 * equal to the number of rows in {@code m}.
	 * </p>
	 * 
	 * @param m another {@code Matrix} suitable for multiplication
	 * @return a {@code Matrix}, which is the result of multiplying {@code this} by
	 *         {@code m}
	 */

	public RectangularMatrix multiply(RectangularMatrix m) {
		if (this.x[0].length != m.x.length)
			throw new IllegalArgumentException(Messages.getString("Matrix.MultiplicationError") + this + " and " + m);

		final int mm = this.x.length;
		final int nn = m.x[0].length;

		var y = new double[mm][nn];

		for (int i = 0; i < mm; i++) {
			for (int j = 0; j < nn; j++) {
				for (int k = 0; k < this.x[0].length; k++) {
					y[i][j] += this.x[i][k] * m.x[k][j];
				}
			}
		}

		return createMatrix(y);

	}

	/**
	 * Scales this {@code Matrix} by {@code f}, which results in element-wise
	 * multiplication by {@code f}.
	 * 
	 * @param f a numeric value
	 * @return the scaled {@code Matrix}
	 */

	public RectangularMatrix multiply(double f) {
		double[][] y = new double[x.length][x[0].length];

		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				y[i][j] = this.x[i][j] * f;
			}
		}

		return createMatrix(y);

	}

	/**
	 * Transposes this {@code Matrix}, i.e. reflects it over the main diagonal.
	 * 
	 * @return a transposed {@code Matrix}
	 */

	public RectangularMatrix transpose() {
		int m = x.length;
		int n = x[0].length;
		double[][] y = new double[n][m];

		for (int i = 0; i < m; i++) {
			for (int j = 0; j < n; j++) {
				y[j][i] = x[i][j];
			}
		}

		return createMatrix(y);

	}

	public double get(int m, int k) {
		return x[m][k];
	}

	public double[][] getData() {
		return x;
	}

	private static RectangularMatrix performOperation(RectangularMatrix m1, RectangularMatrix m2,
			ArithmeticOperation op) {
		if (!m1.dimensionsMatch(m2))
			throw new IllegalArgumentException(Messages.getString("Matrix.DimensionError") + m1 + " != " + m2);

		double[][] y = new double[m1.x.length][m1.x[0].length];

		for (int i = 0; i < y.length; i++) {
			for (int j = 0; j < y[0].length; j++) {
				y[i][j] = op.evaluate(m1.x[i][j], m2.x[i][j]);
			}
		}

		return createMatrix(y);
	}

	/**
	 * <p>
	 * Multiplies this {@code Matrix} by the vector {@code v}, which is represented
	 * by a <math><i>n</i> &times; 1</math> {@code Matrix}, where {@code n} is the
	 * dimension of {@code v}. Note {@code n} should be equal to the number of rows
	 * in this {@code Matrix}.
	 * </p>
	 * 
	 * @param v a {@code Vector}.
	 * @return the result of multiplication, which is a {@code Vector}.
	 */

	public Vector multiply(Vector v) {
		double[] r = new double[x.length];

		if (x[0].length != v.dimension())
			throw new IllegalArgumentException(
					"Cannot multiply a " + x.length + "x" + x[0].length + " matrix by a " + v.dimension() + " vector");

		for (int i = 0; i < x.length; i++) {
			for (int k = 0; k < x[0].length; k++) {
				r[i] += x[i][k] * v.get(k);
			}
		}

		return new Vector(r);
	}

	/**
	 * Prints out matrix dimensions and all the elements contained in it.
	 */

	@Override
	public String toString() {
		int m = x.length;
		int n = x[0].length;
		final String f = Messages.getString("Math.DecimalFormat");

		StringBuilder sb = new StringBuilder(m + "x" + n + " matrix: ");
		for (int i = 0; i < m; i++) {
			sb.append(System.lineSeparator());
			for (int j = 0; j < n; j++) {
				sb.append(" ");
				sb.append(String.format(f, x[i][j]));
			}
		}

		return sb.toString();
	}

	/**
	 * Checks if the dimension of {@code this Matrix} and {@code m} match, i.e. if
	 * the number of rows is the same and the number of columns is the same
	 * 
	 * @param m another {@code Matrix}
	 * @return {@code true} if the dimensions match, {@code false} otherwise.
	 */

	public boolean dimensionsMatch(RectangularMatrix m) {
		return (x.length == m.x.length) && (x[0].length == m.x[0].length);
	}

	/**
	 * Checks whether {@code o} is a {@code SquareMatrix} with matching dimensions
	 * and all elements of which are (approximately) equal to the respective
	 * elements of {@code this} matrix}.
	 */

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SquareMatrix))
			return false;

		if (o == this)
			return true;

		var m = (SquareMatrix) o;

		if (!this.dimensionsMatch(m))
			return false;

		boolean result = true;

		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				if (!approximatelyEquals(this.x[i][j], m.x[i][j])) {
					result = false;
					break;
				}
			}
		}

		return result;

	}

}