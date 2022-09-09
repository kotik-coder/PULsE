package pulse.input;

import static java.util.Objects.requireNonNull;

import java.util.List;

/**
 * Provides a way to define the indices corresponding to a certain range of
 * data.
 * <p>
 * Essentially, an object of this class contains an ordered pair representing
 * the associated indices. Works in conjunction with the {@code Range} class.
 * </p>
 *
 * @see pulse.input.Range
 *
 */
public class IndexRange {

    private int iStart;
    private int iEnd;

    public IndexRange(IndexRange other) {
        iStart = other.iStart;
        iEnd = other.iEnd;
    }
    
    public IndexRange(int start, int end) {
        this.iStart = start;
        this.iEnd = end;
    }

    /**
     * Constructs a new index range for {@code data} based on the dimensional
     * {@code range}.
     *
     * @param data the list to be analysed
     * @param range the range object used to define the index range
     *
     * @see set
     */
    public IndexRange(List<Double> data, Range range) {
        set(data, range);
    }

    /**
     * Resets the index range by effectively treating the {@code data} list as
     * bounded by its first and last elements, assuming that {@code data} is
     * sorted in ascending order. Because of this last assumption, the
     * visibility of this method has been set to protected.
     *
     * @param data a list sorted in ascending order
     */
    protected void reset(List<Double> data) {
        requireNonNull(data);
        int size = data.size();

        if (size > 0) {
            setLowerBound(data, data.get(0));
            setUpperBound(data, data.get(size - 1));
        }

    }

    /**
     * Sets the start index by conducting a primitive binary search using
     * {@code closest(...)} to find an element in {@code data} either matching
     * or being as close as possible to {@code a} (if {@code a} is non-negative)
     * or zero.
     *
     * @param data the list to process
     * @param a an element representing the lower bound (not necessarily
     * contained in {@code data}).
     * @see closestLeft
     * @see closestRight
     */
    public final void setLowerBound(List<Double> data, double a) {
        iStart = a > 0 ? closestLeft(a, data) : closestRight(0, data);
    }

    /**
     * Sets the end index by conducting a primitive binary search using
     * {@code closest(...)} to find an element in {@code data} either matching
     * or being as close as possible to {@code b}. For the above operation, the
     * list is searched through from its last to first element (i.e., in reverse
     * order).
     *
     * @param data the list to process
     * @param b an element representing the upper bound (not necessarily
     * contained in {@code data}).
     * @see closestLeft
     * @see closestRight
     */
    public final void setUpperBound(List<Double> data, double b) {
        iEnd = closestRight(b, data);
    }

    /**
     * Sets the bounds of this index range using the minimum and maximum values
     * of the segment specified in the {@code range} object. If the minimum
     * bound is negative, it will be ignored and replaced by 0.0.
     *
     * @param data the data list to be processed
     * @param range a range with minimum and maximum values
     * @see setLowerBound
     * @see setUpperBound
     */
    public final void set(List<Double> data, Range range) {
        var segment = range.getSegment();
        setLowerBound(data, segment.getMinimum());
        setUpperBound(data, segment.getMaximum());
    }

    /**
     * Gets the integer value representing the index of the lower bound
     * previously set by looking at a certain unspecified data list.
     *
     * @return the start index
     */
    public final int getLowerBound() {
        return iStart;
    }

    /**
     * Gets the integer value representing the index of the upper bound
     * previously set by looking at a certain unspecified data list.
     *
     * @return the end index
     */
    public final int getUpperBound() {
        return iEnd;
    }

    /**
     * Checks if this index range is viable.
     *
     * @return {@code true} if the upper bound is positive and greater than the
     * lower bound, {@code false} otherwise.
     */
    public boolean isValid() {
        return (iStart < iEnd && iEnd > 0);
    }

    /**
     * Searches through the elements contained in the the second argument of
     * this method to find an element belonging to {@code in} most closely
     * resembling the first argument. The search is completed once {@code of}
     * lies between any two adjacent elements of {@code in}. The result is then
     * the index of the preceding element.
     *
     * @param of an element which will be compared against
     * @param in a list of data presumably containing an element similar to
     * {@code of}
     * @return
     * <p>
     * any integer greater than 0 and lesser than {@code in.size} that matches
     * the above criterion. If {@code of} is greater than the last element of
     * {@code in}, this will return the latter. Otherwise, if no element
     * matching the criterion is found, returns 0.
     * </p>
     */
    public static int closestLeft(double of, List<Double> in) {
        return closest(of, in, false);
    }

    /**
     * Searches through the elements contained in the the second argument of
     * this method to find an element belonging to {@code in} most closely
     * resembling the first argument. The search utilises a reverse order, i.e.
     * it starts from the last element and goes to the first. The search is
     * completed once {@code of} lies between any two adjacent elements of
     * {@code in}. The result is then the index of the preceding element.
     *
     * @param of an element which will be compared against
     * @param in a list of data presumably containing an element similar to
     * {@code of}
     * @return
     * <p>
     * any integer greater than 0 and lesser than {@code in.size} that matches
     * the above criterion. If {@code of} is greater than the last element of
     * {@code in}, this will return the latter. Otherwise, if no element
     * matching the criterion is found, returns 0.
     * </p>
     */
    public static int closestRight(double of, List<Double> in) {
        return closest(of, in, true);
    }

    private static int closest(double of, List<Double> in, boolean reverseOrder) {
        int sizeMinusOne = in.size() - 1; //has to be non-negative

        int result = 0;

        if (sizeMinusOne < 1) {
            result = 0;
        } else if (of > in.get(sizeMinusOne)) {
            result = sizeMinusOne;
        } else {

            int start = reverseOrder ? sizeMinusOne - 1 : 0;
            int increment = reverseOrder ? -1 : 1;

            for (int i = start; reverseOrder ? (i > -1) : (i < sizeMinusOne); i += increment) {

                if (between(of, in.get(i), in.get(i + 1))) {
                    result = i;
                    break;
                }

            }

        }

        return result;

    }

    private static boolean between(double x, double minValueInclusive, double maxValueInclusive) {
        return (x >= minValueInclusive && x <= maxValueInclusive);
    }

    @Override
    public String toString() {
        return "Index range: from " + iStart + " to " + iEnd;
    }

}
