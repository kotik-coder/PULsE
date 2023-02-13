package pulse.math.filters;

import java.awt.geom.Point2D;
import java.util.List;
import pulse.DiscreteInput;

public class OptimisedRunningAverage extends RunningAverage {

    private static final long serialVersionUID = 1272276960302188392L;

    public OptimisedRunningAverage() {
        super();
    }

    public OptimisedRunningAverage(int reductionFactor) {
        super(reductionFactor);
    }

    @Override
    public List<Point2D> process(DiscreteInput input) {
        var p = super.process(input);
        var optimisableCurve = new OptimisablePolyline(p);
        var task = new PolylineOptimiser(input, optimisableCurve);
        task.run();
        return optimisableCurve.points();
    }

}
