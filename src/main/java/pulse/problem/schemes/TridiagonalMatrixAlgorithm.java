package pulse.problem.schemes;

/**
 * Implements the tridiagonal matrix algorithm (Thomas algorithms) for solving
 * systems of linear equations. Applicable to such systems where the forming
 * matrix has a tridiagonal form.
 *
 */
public class TridiagonalMatrixAlgorithm {

    private final double tau;
    private final double h;

    private double a;
    private double b;
    private double c;

    private final int N;
    private final double[] alpha;
    private final double[] beta;

    public TridiagonalMatrixAlgorithm(Grid grid) {
        tau = grid.getTimeStep();
        N   = grid.getGridDensityValue();
        h   = grid.getXStep();
        alpha   = new double[N + 2];
        beta    = new double[N + 2];               
    }

    /**
     * Calculates the solution {@code V} using the tridiagonal matrix algorithm.
     * This performs a backwards sweep from {@code N - 1} to {@code 0} where
     * {@code N} is the grid density value. The coefficients {@code alpha} and
     * {@code beta} should have been precalculated
     *
     * @param V the array containing the {@code N}th value previously calculated
     * from the respective boundary condition
     */
    public void sweep(double[] V) {
        for (int j = N - 1; j >= 0; j--) {
            V[j] = alpha[j + 1] * V[j + 1] + beta[j + 1];
        }
    }

    /**
     * Calculates the {@code alpha} coefficients as part of the tridiagonal
     * matrix algorithm.
     */
    public void evaluateAlpha() {
        for (int i = 1; i < N; i++) {
            alpha[i + 1] = c / (b - a * alpha[i]);
        }
    }

    public void evaluateBeta(final double[] U) {
        evaluateBeta(U, 2, N + 1);
    }

    /**
     * Calculates the {@code beta} coefficients as part of the tridiagonal
     * matrix algorithm.
     * @param U
     * @param start
     * @param endExclusive
     */
    public void evaluateBeta(final double[] U, final int start, final int endExclusive) {
        for (int i = start; i < endExclusive; i++) {
            beta[i] = beta(U[i - 1] / tau, phi(i - 1), i);
        }
    }

    public double beta(final double f, final double phi, final int i) {
        return (f + phi + a * beta[i - 1]) / (b - a * alpha[i - 1]);
    }

    public double phi(int i) {
        return 0;
    }

    public void setAlpha(final int i, final double alpha) {
        this.alpha[i] = alpha;
    }

    public void setBeta(final int i, final double beta) {
        this.beta[i] = beta;
    }

    public double[] getAlpha() {
        return alpha;
    }

    public double[] getBeta() {
        return beta;
    }

    public void setCoefA(double a) {
        this.a = a;
    }

    public void setCoefB(double b) {
        this.b = b;
    }

    public void setCoefC(double c) {
        this.c = c;
    }

    protected double getCoefA() {
        return a;
    }

    protected double getCoefB() {
        return b;
    }

    protected double getCoefC() {
        return c;
    }
    
    public final double getTimeStep() {
        return tau;
    }
    
    public final int getGridPoints() {
        return N;
    }
    
    public final double getGridStep() {
        return h;
    }

}