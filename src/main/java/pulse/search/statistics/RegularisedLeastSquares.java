package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.OPTIMISER_STATISTIC;

import pulse.tasks.SearchTask;

public class RegularisedLeastSquares extends OptimiserStatistic {

	private double lambda = 1e-4;
	private SumOfSquares sos;
	
	public RegularisedLeastSquares() {
		super();
		sos = new SumOfSquares();
	}
	
	public RegularisedLeastSquares(RegularisedLeastSquares rls) {
		super(rls);
		sos = new SumOfSquares(rls.sos);
		this.lambda = rls.lambda;
	}

	public double getLambda() {
		return lambda;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}
	
	/*
	 * SSR with L2 regularisation
	 */
	
	@Override
	public void evaluate(SearchTask t) {
		sos.evaluate(t);
		final double ssr = (double)sos.getStatistic().getValue();
		final double statistic = ssr + lambda*t.searchVector().lengthSq();
		setStatistic(derive(OPTIMISER_STATISTIC, statistic));
	}

	@Override
	public String getDescriptor() {
		return "<html><i>L</i><sub>2</sub> Regularised Least Squares</html>";
	}
	
	@Override
	public double variance() {
		return (double)sos.getStatistic().getValue();
	}
	
	@Override
	public OptimiserStatistic copy() {
		return new RegularisedLeastSquares(this);
	}
	
}