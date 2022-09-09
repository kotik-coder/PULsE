package pulse.search.statistics;

import pulse.input.IndexRange;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;
import pulse.search.GeneralTask;

/**
 * This is an experimental feature.
 *
 */
public class RangePenalisedLeastSquares extends SumOfSquares {

    private double lambda = 0.1;
   
    public RangePenalisedLeastSquares() {
        super();
    }

    public RangePenalisedLeastSquares(RangePenalisedLeastSquares rls) {
        super(rls);
        this.lambda = rls.lambda;
    }

    /**
     * The lambda is the regularisation strength.
     *
     * @return the lambda factor.
     */
    public double getLambda() {
        return lambda;
    }

    public void setLambda(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public void evaluate(GeneralTask t) {
        calculateResiduals(t);
        super.evaluate(t);
        final double ssr = (double) getStatistic().getValue();
        var x = t.getInput().getX();
        double partialRange = t.getInput().bounds().length();
        double fullRange = x.get(x.size() - 1) - x.get(IndexRange.closestLeft(0.0, x));
        final double statistic = ssr + lambda * (fullRange - partialRange)/fullRange;
        setStatistic(derive(OPTIMISER_STATISTIC, statistic));
    }

    @Override
    public String getDescriptor() {
        return "<html>Range-Penalised Least Squares</html>";
    }
    
    @Override
    public OptimiserStatistic copy() {
        return new RangePenalisedLeastSquares(this);
    }

}
