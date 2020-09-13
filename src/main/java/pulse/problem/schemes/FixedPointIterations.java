package pulse.problem.schemes;

import static java.lang.Math.abs;

public interface FixedPointIterations {

	public default void doIterations(double[] V, final double error, final int m) {

		final int N = V.length - 1;

		for (double V_0 = error + 1, V_N = error + 1; abs(V[0] - V_0) > error
				|| abs(V[N] - V_N) > error; finaliseIteration(V)) {

			V_N = V[N];
			V_0 = V[0];
			iteration(m);

		}
	}

	public void iteration(final int m);

	public default void finaliseIteration(double[] V) {
		// do nothing
	}

}