package pulse.math.filters;

import java.awt.geom.Point2D;
import java.util.List;
import pulse.DiscreteInput;

public interface Filter {

    public List<Point2D> process(List<Point2D> input);           
    public default List<Point2D> process(DiscreteInput input) {
        return process(DiscreteInput.convert(input.getX(), input.getY()));
    }    
    
}