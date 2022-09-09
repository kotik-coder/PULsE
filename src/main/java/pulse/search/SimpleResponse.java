package pulse.search;

import pulse.Response;
import pulse.math.Segment;
import pulse.search.statistics.OptimiserStatistic;

public abstract class SimpleResponse implements Response {

    private OptimiserStatistic rs;

    public SimpleResponse(OptimiserStatistic rs) {
        setOptimiserStatistic(rs);
    }

    @Override
    public final OptimiserStatistic getOptimiserStatistic() {
        return rs;
    }

    public final void setOptimiserStatistic(OptimiserStatistic statistic) {
        this.rs = statistic;
    }

    @Override
    public double objectiveFunction(GeneralTask task) {
        rs.evaluate(task);
        return (double) rs.getStatistic().getValue();
    }

    @Override
    public Segment accessibleRange() {
        return Segment.UNBOUNDED;
    }

}
