package pulse.math.linear;

/**
 * A static factory class used to create matrices. The factory methods are
 * invoked by classes outside this package instead of the constructors, which
 * are package-private.
 *
 */
public class Matrices {

    private Matrices() {
        // intentionally blank
    }

    /**
     * Creates a square matrix out of {@code data}. Depending on the data
     * dimensions, this will either create a general-form {@code SquareMatrix}
     * or one of the subclasses: {@code Matrix2}, {@code Matrix3} or
     * {@code Matrix4}.
     *
     * @param data the input data
     * @return a {@code} SquareMatrix instance or one of its subclasses
     */
    public static RectangularMatrix createMatrix(double[][] data) {
        return data.length == data[0].length ? createSquareMatrix(data) : new RectangularMatrix(data);
    }

    public static SquareMatrix createSquareMatrix(double[][] data) {
        int m = data.length;

        SquareMatrix result;

        switch (m) {
            case 2:
                result = new Matrix2(data);
                break;
            case 3:
                result = new Matrix3(data);
                break;
            case 4:
                result = new Matrix4(data);
                break;
            default:
                result = new SquareMatrix(data);
        }

        return result;

    }

    /**
     * Creates an identity matrix with its dimension equal to the argument
     *
     * @param dimension the dimension
     * @return an identity matrix of the given dimension
     */
    public static SquareMatrix createIdentityMatrix(int dimension) {
        var data = new double[dimension][dimension];

        for (int i = 0; i < dimension; i++) {
            data[i][i] = 1.0;
        }

        return createSquareMatrix(data);
    }

}
