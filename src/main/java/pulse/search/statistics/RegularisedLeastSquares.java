package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;
import pulse.search.GeneralTask;

/**
 * This is an experimental feature. The objective function here is equal to the
 * ordinary least-square (OLS) plus a penalising term proportional to the
 * squared length of a search vector. This way, search vectors of lower
 * dimensionality are favoured.
 *
 */
public class RegularisedLeastSquares extends SumOfSquares {

    private static final long serialVersionUID = -7398979361944447180L;
    private double lambda = 1e-4;

    public RegularisedLeastSquares() {
        super();
    }

    public RegularisedLeastSquares(RegularisedLeastSquares rls) {
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

    /*
	 * OLS with L2 regularisation. The penalisation term is equal to {@code lambda} times the 
	 * L2 norm of the search vector. 
	 * @see pulse.search.statistics.SumOfSquares
     */
    @Override
    public void evaluate(GeneralTask t) {
        calculateResiduals(t);
        super.evaluate(t);
        final double ssr = (double) getStatistic().getValue();
        final double statistic = ssr + lambda * t.searchVector().toVector().lengthSq();
        setStatistic(derive(OPTIMISER_STATISTIC, statistic));
    }

    @Override
    public String getDescriptor() {
        return "<html><i>L</i><sub>2</sub> Regularised Least Squares</html>";
    }

    @Override
    public OptimiserStatistic copy() {
        return new RegularisedLeastSquares(this);
    }

}
