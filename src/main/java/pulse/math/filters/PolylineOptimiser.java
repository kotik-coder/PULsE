package pulse.math.filters;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.interpolation.UnivariateInterpolator;
import pulse.DiscreteInput;
import pulse.Response;
import pulse.math.ParameterIdentifier;
import pulse.math.ParameterVector;
import pulse.math.Segment;
import pulse.math.linear.Vector;
import pulse.search.SimpleOptimisationTask;
import pulse.search.SimpleResponse;
import pulse.search.direction.BFGSOptimiser;
import pulse.search.statistics.AbsoluteDeviations;
import pulse.search.statistics.OptimiserStatistic;

public class PolylineOptimiser extends SimpleOptimisationTask {

    private final OptimiserStatistic sos;
    private final PolylineResponse response;
    private final OptimisablePolyline optimisableCurve;

    public PolylineOptimiser(DiscreteInput di, OptimisablePolyline optimisableCurve) {
        super(optimisableCurve, di);
        this.sos = new AbsoluteDeviations() {

            @Override
            public void calculateResiduals(DiscreteInput reference, Response estimate) {
                int min = 0;
                int max = reference.getX().size();
                calculateResiduals(reference, estimate, min, max);
            }

        };
        this.optimisableCurve = optimisableCurve;
        response = new PolylineResponse(sos);
        optimisableCurve.addAssignmentListener(() -> response.update(optimisableCurve));
    }

    @Override
    public void setDefaultOptimiser() {
        setOptimiser(BFGSOptimiser.getInstance());
    }

    @Override
    public Response getResponse() {
        return response;
    }

    @Override
    public ParameterVector searchVector() {
        var y = optimisableCurve.getY();
        List<ParameterIdentifier> ids
                = IntStream.range(0, optimisableCurve.getX().length).sequential()
                        .mapToObj(i -> new ParameterIdentifier(i))
                        .collect(Collectors.toList());
        var pv = new ParameterVector(ids);
        pv.setValues(new Vector(y));
        var pvParams = pv.getParameters();
        for (int i = 0; i < pv.dimension(); i++) {
            pvParams.get(i).setBounds(new Segment(y[i] - 2, y[i] + 2));
        }
        return pv;
    }

    public class PolylineResponse extends SimpleResponse {

        UnivariateInterpolator interp;
        UnivariateFunction func;

        public PolylineResponse(OptimiserStatistic os) {
            super(os);
        }

        public void update(OptimisablePolyline impl) {
            interp = new SplineInterpolator();
            func = interp.interpolate(impl.getX(), impl.getY());
        }

        @Override
        public double evaluate(double t) {
            return func.value(t);
        }
    }

}
