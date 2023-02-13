package pulse.math.linear;

/**
 * A square 2-by-2 matrix.
 *
 */
class Matrix2 extends SquareMatrix {

    private static final long serialVersionUID = 6015187791989387058L;

    protected Matrix2(double[][] args) {
        super(args);
    }

    /**
     * Fast (in terms of required floating point operations) calculation of the
     * matrix inverse.
     */
    @Override
    public SquareMatrix inverse() {
        double[][] mx = new double[2][2];
        final var x = this.getData();

        final double det = det();

        mx[0][0] = x[1][1] / det;
        mx[0][1] = -x[0][1] / det;
        mx[1][0] = -x[1][0] / det;
        mx[1][1] = x[0][0] / det;

        return new SquareMatrix(mx);
    }

    /**
     * Fast (in terms of required floating point operations) calculation of the
     * determinant.
     */
    @Override
    public double det() {
        var x = getData();
        return x[0][0] * x[1][1] - x[1][0] * x[0][1];
    }

}
