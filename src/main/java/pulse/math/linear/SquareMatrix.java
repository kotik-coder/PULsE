package pulse.math.linear;

import static org.ejml.dense.row.CommonOps_DDRM.invert;
import static pulse.math.linear.Matrices.createSquareMatrix;

import org.ejml.data.DMatrixRMaj;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.dense.row.MatrixFeatures_DDRM;

/**
 * The matrix class.
 * <p>
 * Used for operations on small matrices primarily in optimisation and
 * Runge-Kutta calculations of radiative transfer. Fixed-size matrices of size
 * 2, 3, and 4 are the direct subclasses and offer a boost in performance by
 * implementing a reduced FLOP matrix inverse.
 * </p>
 * <p>
 * Note this class cannot be instantiated directly from outside the
 * {@code pulse.math} package, the user needs to invoke the factory class
 * methods {@code Matrices} instead.
 * </p>
 * 
 * @see pulse.math.linear.Matrices
 */

public class SquareMatrix extends RectangularMatrix {

	/**
	 * Constructs a {@code Matrix} with the elements copied from {@code args}. The
	 * elements are copied by invoking System.arraycopy(...).
	 * 
	 * @param args a two-dimensional double array
	 */

	protected SquareMatrix(double[][] args) {
		super(args);
	}

	private SquareMatrix(double[] data, int n) {
		super(data, n);
	}

	/**
	 * Calculates the determinant for an <i>n</i>-by-<i>n</i> square matrix. The
	 * determinant is calculated using the EJML library.
	 * 
	 * @return a double, representing the determinant
	 */

	public double det() {
		var mx = new DMatrixRMaj(x);
		return CommonOps_DDRM.det(mx);
	}

	/**
	 * Conducts matrix inversion with the procedural EJML approach. Can be overriden
	 * by subclasses to boost performance.
	 * 
	 * @return the inverted {@Code Matrix}.
	 */

	public SquareMatrix inverse() {
		var mx = new DMatrixRMaj(x);
		invert(mx);
		return new SquareMatrix(mx.getData(), x.length);
	}
	
	/**
	 * Checks if a matrix is positive definite. Uses EJML implementation.
	 * @return {@code true} is positive-definite
	 */
	
	public boolean isPositiveDefinite() {
		return MatrixFeatures_DDRM.isPositiveDefinite(new DMatrixRMaj(x));
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

		for (int i = 0; i < x.length; i++) {
			for (int j = 0; j < x[0].length; j++) {
				x[i][j] = a.get(i) * b.get(j);
			}
		}

		return createSquareMatrix(x);
	}
	
	public static SquareMatrix asSquareMatrix(RectangularMatrix m) {
		return m.x.length == m.x[0].length ? new SquareMatrix(m.getData()) : null;
	}

}