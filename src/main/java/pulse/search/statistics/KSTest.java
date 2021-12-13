package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.PROBABILITY;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.inference.TestUtils;

import pulse.tasks.SearchTask;

/**
 * The Kolmogorov-Smirnov normality test as implemented in
 * {@code ApacheCommonsMath}.
 *
 */
public class KSTest extends NormalityTest {

    private double[] residuals;
    private NormalDistribution nd;

    @Override
    public boolean test(SearchTask task) {
        evaluate(task);
        setProbability(derive(PROBABILITY, TestUtils.kolmogorovSmirnovTest(nd, residuals)));
        return significanceTest();
    }

    @Override
    public void evaluate(SearchTask t) {
        calculateResiduals(t);
        residuals = transformResiduals();

        final double sd = (new StandardDeviation()).evaluate(residuals);
        nd = new NormalDistribution(0.0, sd); // null hypothesis: normal distribution with zero mean and empirical
        // standard dev
        final double statistic = TestUtils.kolmogorovSmirnovStatistic(nd, residuals);
        this.setStatistic(derive(TEST_STATISTIC, statistic));
    }

    @Override
    public String getDescriptor() {
        return "Kolmogorov-Smirnov test";
    }

}
