package pulse.math.linear;

/**
 * A 4-by-4 matrix.
 *
 */

class Matrix4 extends SquareMatrix {

	protected Matrix4(double[][] args) {
		super(args);
	}
	
	/**
	 * Fast inverse procedure for 4x4 matrix. Credit to Robin Hilliard.
	 * 
	 * @return inverse of a 4x4 matrix
	 */
	
	@Override
	public SquareMatrix inverse() {
		final var x	= getData();
		var mx		= new double[4][4];

		final double s0 = x[0][0] * x[1][1] - x[1][0] * x[0][1];
		final double s1 = x[0][0] * x[1][2] - x[1][0] * x[0][2];
		final double s2 = x[0][0] * x[1][3] - x[1][0] * x[0][3];
		final double s3 = x[0][1] * x[1][2] - x[1][1] * x[0][2];
		final double s4 = x[0][1] * x[1][3] - x[1][1] * x[0][3];
		final double s5 = x[0][2] * x[1][3] - x[1][2] * x[0][3];

		final double c5 = x[2][2] * x[3][3] - x[3][2] * x[2][3];
		final double c4 = x[2][1] * x[3][3] - x[3][1] * x[2][3];
		final double c3 = x[2][1] * x[3][2] - x[3][1] * x[2][2];
		final double c2 = x[2][0] * x[3][3] - x[3][0] * x[2][3];
		final double c1 = x[2][0] * x[3][2] - x[3][0] * x[2][2];
		final double c0 = x[2][0] * x[3][1] - x[3][0] * x[2][1];

		// Should check for 0 determinant

		final double invdet = 1.0 / (s0 * c5 - s1 * c4 + s2 * c3 + s3 * c2 - s4 * c1 + s5 * c0);

		mx[0][0] = (x[1][1] * c5 - x[1][2] * c4 + x[1][3] * c3) * invdet;
		mx[0][1] = (-x[0][1] * c5 + x[0][2] * c4 - x[0][3] * c3) * invdet;
		mx[0][2] = (x[3][1] * s5 - x[3][2] * s4 + x[3][3] * s3) * invdet;
		mx[0][3] = (-x[2][1] * s5 + x[2][2] * s4 - x[2][3] * s3) * invdet;

		mx[1][0] = (-x[1][0] * c5 + x[1][2] * c2 - x[1][3] * c1) * invdet;
		mx[1][1] = (x[0][0] * c5 - x[0][2] * c2 + x[0][3] * c1) * invdet;
		mx[1][2] = (-x[3][0] * s5 + x[3][2] * s2 - x[3][3] * s1) * invdet;
		mx[1][3] = (x[2][0] * s5 - x[2][2] * s2 + x[2][3] * s1) * invdet;

		mx[2][0] = (x[1][0] * c4 - x[1][1] * c2 + x[1][3] * c0) * invdet;
		mx[2][1] = (-x[0][0] * c4 + x[0][1] * c2 - x[0][3] * c0) * invdet;
		mx[2][2] = (x[3][0] * s4 - x[3][1] * s2 + x[3][3] * s0) * invdet;
		mx[2][3] = (-x[2][0] * s4 + x[2][1] * s2 - x[2][3] * s0) * invdet;

		mx[3][0] = (-x[1][0] * c3 + x[1][1] * c1 - x[1][2] * c0) * invdet;
		mx[3][1] = (x[0][0] * c3 - x[0][1] * c1 + x[0][2] * c0) * invdet;
		mx[3][2] = (-x[3][0] * s3 + x[3][1] * s1 - x[3][2] * s0) * invdet;
		mx[3][3] = (x[2][0] * s3 - x[2][1] * s1 + x[2][2] * s0) * invdet;

		return new SquareMatrix(mx);

	}
	
}