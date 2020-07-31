package pulse.math.linear;

import static org.ejml.dense.row.CommonOps_DDRM.invert;
import static pulse.math.linear.ArithmeticOperations.difference;
import static pulse.math.linear.ArithmeticOperations.sum;
import static pulse.math.linear.Matrices.createMatrix;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;

import pulse.ui.Messages;

/**
 * The matrix class.
 * <p>
 * Used for operations on small matrices (generally, not exceeding a
 * size of 10), used primarily in optimisation and Runge-Kutta calculations of
 * radiative transfer. Specific subclasses exist for matrices of size 2, 3, and 4,
 * which are used to boost performance in some cases.
 * </p>
 * 
 * <p>
 * Note this class cannot be instantiated directly from outside the {@code pulse.math} package,
 * the user needs to invoke the factory class methods {@code Matrices} instead.
 * 
 * @see pulse.math.linear.Matrices
 */

public class SquareMatrix {

	private final double[][] x;

	/**
	 * Constructs a {@code Matrix} with the elements copied from {@code args}. The
	 * elements are copied by invoking System.arraycopy(...).
	 * 
	 * @param args a two-dimensional double array
	 */

	protected SquareMatrix(double[][] args) {
		int m = args.length;
		int n = args[0].length;

		x = new double[m][n];

		for (int i = 0; i < m; i++)
			System.arraycopy(args[i], 0, x[i], 0, n);

	}

	private SquareMatrix(double[] data, int n) {
		this.x = new double[n][n];

		for (int i = 0; i < n; i++) 
			System.arraycopy(data, i*n, x[i], 0, n);
	
	}

	/**
	 * <p>
	 * Checks whether both {@code this} and {@code m} have matching dimensions. If
	 * true, will perform an element-wise summation.
	 * </p>
	 * 
	 * @param m another {@code Matrix} of the same size as {@code this} one
	 * @return a {@code Matrix} with elements formed as the result of summing up the
	 *         respective elements in {@code this} and {@code m}
	 */

	public SquareMatrix sum(SquareMatrix m) {
		return performOperation(this, m, sum);
	}

	/**
	 * <p>
	 * Checks whether both {@code this} and {@code m} have matching dimensions. If
	 * true, will perform an element-wise subtraction.
	 * </p>
	 * 
	 * @param m another {@code Matrix} of the same size as {@code this} one
	 * @return a {@code Matrix} with elements formed as the result of subtracting an
	 *         element of matrix {@code m} from the respective element in
	 *         {@code this}
	 */

	public SquareMatrix subtract(SquareMatrix m) {
		return performOperation(this, m, difference);
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

	public SquareMatrix multiply(SquareMatrix m) {
		if (this.x[0].length != m.x.length)
			throw new IllegalArgumentException(Messages.getString("Matrix.MultiplicationError") + this + " and " + m);

		int mm = this.x.length;
		int nn = m.x[0].length;

		double[][] y = new double[mm][nn];

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

	public SquareMatrix multiply(double f) {
		double[][] y = new double[this.x[0].length][this.x[0].length];

		for (int i = 0; i < x[0].length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				y[i][j] = this.x[i][j] * f;
			}
		}

		return createMatrix(y);
		
	}

	/**
	 * <p>
	 * Multiplies this {@code Matrix} by the vector {@code v}, which is represented
	 * by a {@code n x 1} {@code Matrix}, where {@code n} is the dimension of
	 * {@code v}. Note {@code n} should be equal to the number of rows in this
	 * {@code Matrix}.
	 * </p>
	 * 
	 * @param v a {@code Vector}.
	 * @return the result of multiplication, which is a {@code Vector}.
	 */

	public Vector multiply(Vector v) {
		double[] r = new double[v.dimension()];

		for (int i = 0; i < r.length; i++) {
			for (int j = 0; j < r.length; j++) {
				r[i] += x[i][j] * v.get(j);
			}
		}

		return new Vector(r);
	}

	/**
	 * Transposes this {@code Matrix}, i.e. reflects it over the main diagonal.
	 * 
	 * @return a transposed {@code Matrix}
	 */

	public SquareMatrix transpose() {
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

	/**
	 * Calculates the determinant for an <i>n</i>-by-<i>n</i> square matrix.
	 * The determinant is calculated using the EJML library.
	 * 
	 * @return a double, representing the determinant
	 */

	public double det() {
		var mx = new DMatrixRMaj(x);
		return CommonOps_DDRM.det(mx);
	}

	/**
	 * Conducts matrix inversion with the procedural EJML approach.
	 * Can be overriden by subclasses to boost performance.
	 * 
	 * @return the inverted {@Code Matrix}.
	 */

	public SquareMatrix inverse() {
		var mx = new DMatrixRMaj(x);
		invert(mx);
		return new SquareMatrix(mx.getData(), x.length);
	}

	/**
	 * Calculates the outer product of two vectors.
	 * 
	 * @param a a Vector
	 * @param b a Vector
	 * @return the outer product of {@code a} and {@code b}
	 */

	public static SquareMatrix outerProduct(Vector a, Vector b) {
		double[][] x = new double[a.dimension()][b.dimension()];
		
		for(int i = 0; i < x.length; i++) {
			for(int j = 0; j < x[0].length; j++) {
				x[i][j] = a.get(i) * b.get(j);
			}
		}
		
		return createMatrix(x);
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof SquareMatrix))
			return false;

		if (o == this)
			return true;

		var m = (SquareMatrix) o;

		if (m.x.length != m.x[0].length)
			return false;

		final double EPS = 1E-8;

		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x.length; j++) {
				if (Math.abs(this.x[i][j] - m.x[i][j]) > EPS)
					return false;
			}
		}

		return true;
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

	public boolean hasSameDimensions(SquareMatrix m) {

		if (this.x.length != m.x.length)
			return false;

		return this.x[0].length == m.x[0].length;

	}

	public double get(int m, int k) {
		return x[m][k];
	}
	
	public double[][] getData() {
		return x;
	}

	private static SquareMatrix performOperation(SquareMatrix m1, SquareMatrix m2, ArithmeticOperation op) {
		if (!m1.hasSameDimensions(m2))
			throw new IllegalArgumentException(Messages.getString("Matrix.DimensionError") + m1 + " != " + m2);

		double[][] y = new double[m1.x.length][m1.x[0].length];

		for (int i = 0; i < m1.x.length; i++) {
			for (int j = 0; j < m1.x[0].length; j++) {
				y[i][j] = op.evaluate(m1.x[i][j], m2.x[i][j]);
			}
		}

		return createMatrix(y);
	}

}