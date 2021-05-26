package pulse.problem.schemes;

/**
 * A modification of the algorithm for solving a system of linear equations, where the first and last equation contains references 
 * to the last and first elements of the solution, respectively. The corresponding matrix is composed of an inner tridiagonal block 
 * and a border formed by an extra row and column. This block system is solved using the Sherman-Morrison-Woodbury identity and the 
 * Thomas algorithm for the main block. 
 *
 */

public class BlockMatrixAlgorithm extends TridiagonalMatrixAlgorithm {

	private double[] gamma;
	private double[] p;
	private double[] q;

	public BlockMatrixAlgorithm(Grid grid) {
		super(grid);
		gamma = new double[getAlpha().length];
		p = new double[gamma.length - 1];
		q = new double[gamma.length - 1];
	}
	
	@Override
	public void sweep(double[] V) {
		final int N = V.length - 1;
		for (int j = N - 1; j >= 0; j--)
			V[j] = p[j] + V[N] * q[j];
	}

	@Override
	public void evaluateBeta(final double[] U) {
		super.evaluateBeta(U);
		final int N = getGrid().getGridDensityValue();
		var alpha = getAlpha();
		var beta = getBeta();

		p[N - 1] = beta[N];
		q[N - 1] = alpha[N] + gamma[N];

		for (int i = N - 2; i >= 0; i--) {
			p[i] = alpha[i + 1] * p[i + 1] + beta[i + 1];
			q[i] = alpha[i + 1] * q[i + 1] + gamma[i + 1];
		}
	}

	@Override
	public void evaluateBeta(final double[] U, final int start, final int endExclusive) {
		var alpha = getAlpha();
		var grid = getGrid();
		final double HX2_TAU = grid.getXStep() * grid.getXStep() / getGrid().getTimeStep();

		final double a = getCoefA();
		final double b = getCoefB();

		for (int i = start; i < endExclusive; i++) {
			setBeta(i, beta(U[i - 1] * HX2_TAU, phi(i - 1), i));
			setGamma(i, a * gamma[i - 1] / (b - a * alpha[i - 1]));
		}

	}

	public double[] getP() {
		return p;
	}

	public double[] getQ() {
		return q;
	}

	public void setGamma(final int i, final double g) {
		this.gamma[i] = g;
	}

	public double[] getGamma() {
		return gamma;
	}

}
