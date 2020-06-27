package pulse.input;

import java.util.List;
import java.util.Objects;

import pulse.HeatingCurve;

public class IndexRange {

	private int iStart, iEnd;

	public IndexRange() {
		iStart = -1;
		iEnd = 0;
	}

	/**
	 * Sets the new time range that will be used in the optimisation problem.
	 * 
	 * @param a the lower time limit satisfying the {@code a > b} relation
	 * @param b the upper time limit
	 */

	public IndexRange(List<Double> data, Range range) {
		set(data, range);
	}

	public void reset(List<Double> data) {
		Objects.requireNonNull(data);
		int size = data.size();

		if (size > 0) {
			setLowerBound(data, data.get(0));
			setUpperBound(data, data.get(size - 1));
		}

	}

	public void setLowerBound(List<Double> data, double a) {
		int count = data.size();

		for (iStart = 0; iStart < count - 1; iStart++) {
			if (data.get(iStart) >= a)
				break;
		}

		if (data.get(iStart) < 0)
			iStart = closest(0, data);

	}

	public void setUpperBound(List<Double> data, double b) {
		int count = data.size();

		for (iEnd = count - 2; iEnd > iStart; iEnd--) {
			if (data.get(iEnd) < b)
				break;
		}

		iEnd++;

	}

	public void set(List<Double> data, Range range) {
		setLowerBound(data, range.getSegment().getMinimum());
		setUpperBound(data, range.getSegment().getMaximum());
	}

	/**
	 * Returns the fitting start index, or the lower boundary of the time domain
	 * used in the solution of the reverse heat problem. This index can only point
	 * to a non-negative moment in time {@code t >= 0}.
	 * 
	 * @return an integer, specifying the fitting start index.
	 */

	public int getLowerBound() {
		return iStart;
	}

	/**
	 * Returns the fitting end index, or the upper boundary of the time domain used
	 * in the solution of the reverse heat problem.
	 * 
	 * @return an integer, specifying the fitting end index.
	 */

	public int getUpperBound() {
		return iEnd;
	}

	public boolean isValid() {
		return (iStart < iEnd && iEnd > 0);
	}

	/*
	 * return the index of the list element which is closest to t
	 */

	public static int closest(double of, List<Double> in) {
		int size = in.size();

		for (int i = 0; i < size - 1; i++) {
			if (of >= in.get(i))
				if (of < in.get(i + 1))
					return i;
		}
		return of > in.get(size - 1) ? size - 1 : -1;

	}

	public int count() {
		return iEnd - iStart;
	}

	@Override
	public String toString() {
		return "Index range: from " + iStart + " to " + iEnd;
	}

}