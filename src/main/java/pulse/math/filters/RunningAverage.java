package pulse.math.filters;

import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

public class RunningAverage implements Filter {

    private static final long serialVersionUID = -6134297308468858848L;

    private int bins;

    /**
     * The binning factor used to build a crude approximation of the heating
     * curve. Described in Lunev, A., &amp; Heymer, R. (2020). Review of
     * Scientific Instruments, 91(6), 064902.
     */
    public static final int DEFAULT_BINS = 16;
    public final static int MIN_BINS = 4;

    /**
     * @param reductionFactor the factor, by which the number of points
     * {@code count} will be reduced for this {@code ExperimentalData}.
     */
    public RunningAverage(int reductionFactor) {
        this.bins = reductionFactor;
    }

    public RunningAverage() {
        this.bins = DEFAULT_BINS;
    }

    /**
     * Constructs a deliberately crude representation of this heating curve by
     * calculating a running average.
     * <p>
     * This is done using a binning algorithm, which will group the
     * time-temperature data associated with this {@code ExperimentalData} in
     * {@code count/reductionFactor - 1} bins, calculate the average value for
     * time and temperature within each bin, and collect those values in a
     * {@code List<Point2D>}. This is useful to cancel out the effect of signal
     * outliers, e.g. when calculating the half-rise time.
     * </p>
     *
     * The algorithm is described in more detail in Lunev, A., &amp; Heymer, R.
     * (2020). Review of Scientific Instruments, 91(6), 064902.
     *
     * @param points
     * @param input
     * @return a {@code List<Point2D>}, representing the degraded
     * {@code ExperimentalData}.
     * @see halfRiseTime()
     * @see pulse.AbstractData.maxTemperature()
     */
    @Override
    public List<Point2D> process(List<Point2D> points) {
        var x = points.stream().mapToDouble(p -> p.getX()).toArray();
        var y = points.stream().mapToDouble(p -> p.getY()).toArray();

        int size = x.length;
        int step = size / bins;
        List<Point2D> movingAverage = new ArrayList<>(bins);

        for (int i = 0; i < bins; i++) {
            int i1 = step * i;
            int i2 = step * (i + 1);

            double av = 0;
            int j;

            for (j = i1; j < i2 && j < size; j++) {
                av += y[j];
            }

            av /= j - i1;
            i2 = j - 1;

            movingAverage.add(new Point2D.Double(
                    (x[i1] + x[i2]) / 2.0, av));

        }

        addBoundaryPoints(movingAverage, x[0], x[size - 1]);

        /*
        for(int i = 0; i < movingAverage.size(); i++) {
            System.err.println(movingAverage.get(i));
        }
         */
        return movingAverage;

    }

    private static void addBoundaryPoints(List<Point2D> d, double minTime, double maxTime) {
        int max = d.size();

        d.add(
                extrapolate(d.get(max - 1),
                        d.get(max - 2),
                        maxTime)
        );

        d.add(0,
                extrapolate(d.get(0),
                        d.get(1),
                        minTime)
        );

    }

    private static Point2D extrapolate(Point2D a, Point2D b, double x) {
        double y1 = a.getY();
        double y2 = b.getY();
        double x1 = a.getX();
        double x2 = b.getX();

        return new Point2D.Double(x, y1 + (x - x1) / (x2 - x1) * (y2 - y1));
    }

    public final int getNumberOfBins() {
        return bins;
    }

    public final void setNumberOfBins(int no) {
        this.bins = no > MIN_BINS - 1 ? no : MIN_BINS;
    }

}
