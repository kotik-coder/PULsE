package pulse.math.filters;

import java.awt.geom.Point2D;
import java.util.List;

public class Randomiser implements Filter {

    private static final long serialVersionUID = 3390706390237573886L;
    private final double amplitude;

    public Randomiser(double amplitude) {
        this.amplitude = amplitude;
    }

    @Override
    public List<Point2D> process(List<Point2D> input) {
        input.forEach(p
                -> ((Point2D.Double) p).y += (Math.random() - 0.5) * amplitude
        );
        return input;
    }

    public double getAmplitude() {
        return amplitude;
    }

}
