package pulse.search.statistics;

public class EmptyCorrelationTest extends CorrelationTest {

	@Override
	public double evaluate(double[] x, double[] y) {
		return 0;
	}

	@Override
	public String getDescriptor() {
		return "Don't test please";
	}

}
