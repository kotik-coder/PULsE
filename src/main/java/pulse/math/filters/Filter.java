package pulse.math.filters;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.List;
import pulse.DiscreteInput;

public interface Filter extends Serializable {

    public List<Point2D> process(List<Point2D> input);

    public default List<Point2D> process(DiscreteInput input) {
        return process(DiscreteInput.convert(input.getX(), input.getY()));
    }

}
