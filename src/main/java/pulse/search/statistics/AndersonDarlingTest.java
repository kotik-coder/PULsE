package pulse.search.statistics;

import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericPropertyKeyword.TEST_STATISTIC;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import pulse.search.GeneralTask;

import umontreal.ssj.gof.GofStat;
import umontreal.ssj.probdist.NormalDist;

/**
 * The Anderson-Darling normality test. In this variant of the test, the mean
 * and the variance are assumed to be known.
 *
 */
public class AndersonDarlingTest extends NormalityTest {

    /**
     * This uses the SSJ statistical library to calculate the Anderson-Darling
     * test with the input parameters formed by the {@code task} residuals and a
     * normal distribution with zero mean and variance equal to the residuals
     * variance.
     * @param task
     * @return 
     */
    @Override
    public boolean test(GeneralTask task) {
        calculateResiduals(task);

        double[] residuals = residualsArray();
        var nd = new NormalDist(0.0, (new StandardDeviation()).evaluate(residuals));
        var testResult = GofStat.andersonDarling(residuals, nd);

        this.setStatistic(derive(TEST_STATISTIC, testResult[0]));
        
        //compare the p-value and the significance
        return testResult[1] > significance;
    }

    @Override
    public String getDescriptor() {
        return "Anderson-Darling test";
    }

    @Override
    public void evaluate(GeneralTask t) {
        test(t);
    }

}
