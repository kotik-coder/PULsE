package pulse.search.statistics;

public class EmptyCorrelationTest extends CorrelationTest {

    private static final long serialVersionUID = -2462666081516562018L;

    @Override
    public double evaluate(double[] x, double[] y) {
        return 0;
    }

    @Override
    public String getDescriptor() {
        return "Don't test please";
    }

}
