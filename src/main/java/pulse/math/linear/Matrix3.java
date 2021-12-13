package pulse.math.linear;

/**
 * A 3-by-3 matrix class.
 *
 */
class Matrix3 extends SquareMatrix {

    protected Matrix3(double[][] args) {
        super(args);
    }

    /**
     * Fast (in terms of required floating point operations) calculation of the
     * matrix inverse.
     */
    @Override
    public SquareMatrix inverse() {

        double[][] minv = new double[3][3];
        final var x = getData();

        // computes the inverse of a matrix m
        final double invdet = 1.0 / det();

        minv[0][0] = (x[1][1] * x[2][2] - x[2][1] * x[1][2]) * invdet;
        minv[0][1] = (x[0][2] * x[2][1] - x[0][1] * x[2][2]) * invdet;
        minv[0][2] = (x[0][1] * x[1][2] - x[0][2] * x[1][1]) * invdet;
        minv[1][0] = (x[1][2] * x[2][0] - x[1][0] * x[2][2]) * invdet;
        minv[1][1] = (x[0][0] * x[2][2] - x[0][2] * x[2][0]) * invdet;
        minv[1][2] = (x[1][0] * x[0][2] - x[0][0] * x[1][2]) * invdet;
        minv[2][0] = (x[1][0] * x[2][1] - x[2][0] * x[1][1]) * invdet;
        minv[2][1] = (x[2][0] * x[0][1] - x[0][0] * x[2][1]) * invdet;
        minv[2][2] = (x[0][0] * x[1][1] - x[1][0] * x[0][1]) * invdet;

        return new SquareMatrix(minv);
    }

    @Override
    public double det() {
        var x = getData();
        return x[0][0] * (x[1][1] * x[2][2] - x[2][1] * x[1][2]) - x[0][1] * (x[1][0] * x[2][2] - x[1][2] * x[2][0])
                + x[0][2] * (x[1][0] * x[2][1] - x[1][1] * x[2][0]);
    }

}
