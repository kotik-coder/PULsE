package pulse;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import pulse.input.IndexRange;
import pulse.math.Segment;

public interface DiscreteInput extends Serializable {

    public List<Double> getX();

    public List<Double> getY();

    public IndexRange getIndexRange();

    public static List<Point2D> convert(double[] x, double[] y) {

        var ps = new ArrayList<Point2D>();

        for (int i = 0, size = x.length; i < size; i++) {
            ps.add(new Point2D.Double(x[i], y[i]));
        }

        return ps;

    }

    public static List<Point2D> convert(List<Double> x, List<Double> y) {

        var ps = new ArrayList<Point2D>();

        for (int i = 0, size = x.size(); i < size; i++) {
            ps.add(new Point2D.Double(x.get(i), y.get(i)));
        }

        return ps;

    }

    public default Segment bounds() {
        var ir = getIndexRange();
        var x = getX();
        return new Segment(x.get(ir.getLowerBound()), x.get(ir.getUpperBound()));
    }

}
