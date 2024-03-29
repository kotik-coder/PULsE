package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.apache.commons.math3.stat.inference.TestUtils;
import pulse.search.GeneralTask;

/**
 * The Kolmogorov-Smirnov normality test as implemented in
 * {@code ApacheCommonsMath}.
 *
 */
public class KSTest extends NormalityTest {

    private double[] residuals;
    private NormalDistribution nd;

    @Override
    public boolean test(GeneralTask task) {
        evaluate(task);

        this.setStatistic(derive(TEST_STATISTIC,
                TestUtils.kolmogorovSmirnovStatistic(nd, residuals)));
        return !TestUtils.kolmogorovSmirnovTest(nd, residuals, this.significance);
    }

    @Override
    public void evaluate(GeneralTask t) {
        calculateResiduals(t);
        residuals = residualsArray();

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
