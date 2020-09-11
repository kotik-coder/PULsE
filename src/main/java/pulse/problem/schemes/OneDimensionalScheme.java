package pulse.problem.schemes;

import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;

public abstract class OneDimensionalScheme extends DifferenceScheme {

	private double[] U;
	private double[] V;
	
	protected OneDimensionalScheme() {
		super();
	}

	protected OneDimensionalScheme(NumericProperty timeLimit) {
		super(timeLimit);
	}
	
	@Override
	protected void prepare(Problem problem) {
		super.prepare(problem);
		final int N = (int) getGrid().getGridDensity().getValue();
		U = new double[N + 1];
		V = new double[N + 1];
	}
	
	@Override
	public double signal() {
		return V[ V.length - 1 ];
	}

	@Override
	public void finaliseStep() {
		System.arraycopy(V, 0, U, 0, V.length);
	}
	
	public double[] getPreviousSolution() {
		return U;
	}
	
	public double[] getCurrentSolution() {
		return V;
	}
	
	public void setSolutionAt(final int i, final double v) {
		this.V[i] = v;
	}
	
}