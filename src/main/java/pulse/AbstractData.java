package pulse;

import static java.lang.Math.abs;
import static java.util.Collections.max;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericProperties.derive;
import static pulse.properties.NumericProperty.requireType;
import static pulse.properties.NumericPropertyKeyword.NUMPOINTS;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.util.PropertyHolder;

/**
 * A named collection of time and temperature values, with user-adjustable number of entries. 
 * <p>
 * The notion of temperature is loosely used here, and this can represent just
 * the detector signal in mV. Unless explicitly specified otherwise, the unit of
 * the temperature can be arbitrary, and only the shape of the heating curve
 * matters when calculating the reverse solution of the heat problem.
 * </p>
 *
 */

public abstract class AbstractData extends PropertyHolder {

	private int count;

	private List<Double> time;
	private List<Double> signal;
	
	private String name;
	
	protected AbstractData(List<Double> time, String name) {
		this.time = time;
		this.count = time.size();
		this.name = name;
	}
	
	/**
	 * Creates an {@code AbstractData} with the default number of points (set in the
	 * corresponding XML file).
	 */

	public AbstractData() {
		this(def(NUMPOINTS));
	}

	/**
	 * Creates a {@code AbstractData}, where the number of elements in the
	 * {@code time} and {@code temperature} collections are set to
	 * {@code count.getValue()}.
	 * <p>
	 * 
	 * @param count The {@code NumericProperty} that is derived from the
	 *              {@code NumericPropertyKeyword.NUMPOINTS}.
	 */

	public AbstractData(NumericProperty count) {
		setNumPoints(count);
		time = new ArrayList<>(this.count);
		signal = new ArrayList<>(this.count);		
	}
	
	/**
	 * The actual number of points, explicitly calculated as the size of the internal lists.
	 * @return an integer size equal to the real number of elements (pairs)
	 */
	
	public int actualNumPoints() {
		return signal.size();
	}
	
	/**
	 * Clears all elements from the three {@code List} objects, thus releasing
	 * memory.
	 */

	public void clear() {
		this.time.clear();
		this.signal.clear();
	}
	
	/**
	 * Getter method providing accessibility to the {@code count NumericProperty}.
	 * 
	 * @return a {@code NumericProperty} derived from
	 *         {@code NumericPropertyKeyword.NUMPOINTS} with the value of
	 *         {@code count}
	 */

	public NumericProperty getNumPoints() {
		return derive(NUMPOINTS, count);
	}

	/**
	 * Sets the number of points for this baseline.
	 * <p>
	 * The {@code List} data objects, containing time, temperature, and
	 * baseline-subtracted temperature are filled with zeroes.
	 * 
	 * @param c
	 */

	public void setNumPoints(NumericProperty c) {
		requireType(c, NUMPOINTS);
		this.count = (int) c.getValue();
		firePropertyChanged(this, c);
	}

	/**
	 * Retrieves an element from the {@code time List} specified by {@code index}
	 * 
	 * @param index the index of the element to be returned
	 * @return a time value corresponding to {@code index}
	 */

	public double timeAt(int index) {
		return time.get(index);
	}
	
	/**
	 * Retrieves the last element of the {@code time List}. This is used e.g. by the
	 * {@code DifferenceScheme} to set the calculation limit for the
	 * finite-difference scheme.
	 * 
	 * @see pulse.problem.schemes.DifferenceScheme
	 * @return a double, equal to the last element of the {@code time List}.
	 */

	public double timeLimit() {
		return timeAt(time.size() - 1);
	}

	/**
	 * Retrieves the <b>baseline-subtracted</b> temperature corresponding to
	 * {@code index} in the respective {@code List}.
	 * 
	 * @param index the index of the element
	 * @return a double, respresenting the baseline-subtracted temperature at
	 *         {@code index}
	 */

	public double signalAt(int index) {
		return signal.get(index);
	}

	public void addPoint(double time, double temperature) {
		this.time.add(time);
		this.signal.add(temperature);
	}

	protected void incrementCount() {
		count++;
	}

	/**
	 * Sets the time {@code t} at the position {@code index} of the
	 * {@code time List}.
	 * 
	 * @param index the index
	 * @param t     the new time value at this index
	 */

	public void setTimeAt(int index, double t) {
		time.set(index, t);
	}

	/**
	 * Sets the temperature {@code t} at the position {@code index} of the
	 * {@code temperature List}.
	 * 
	 * @param index the index
	 * @param t     the new temperature value at this index
	 */

	public void setSignalAt(int index, double t) {
		signal.set(index, t);
	}
	
	public double apparentMaximum() {
		return max(signal);
	}

	public boolean isIncomplete() {
		return time.size() < count;
	}
	
	@Override
	public String toString() {
		return name != null ? name : getClass().getSimpleName() + " (" + getNumPoints() + ")";
	}
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Provides general setter accessibility for the number of points of this
	 * {@code AbstractData}.
	 * 
	 * @param type     must be equal to {@code NumericPropertyKeyword.NUMPOINTS}
	 * @param property the property of the type
	 *                 {@code NumericPropertyKeyword.NUMPOINTS}
	 */

	@Override
	public void set(NumericPropertyKeyword type, NumericProperty property) {
		if (type == NUMPOINTS)
			setNumPoints(property);
	}

	@Override
	public List<Property> listedTypes() {
		return new ArrayList<>(Arrays.asList(getNumPoints()));
	}

	/**
	 * Removes an element with the index {@code i} from the time-temperature lists.
	 * 
	 * @param i the element to be removed
	 */

	public void remove(int i) {
		this.time.remove(i);
		this.signal.remove(i);
	}
	
	@Override
	public boolean ignoreSiblings() {
		return true;
	}

	public List<Double> getTimeSequence() {
		return time;
	}

	public List<Double> getSignalData() {
		return signal;
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;

		if (!(o instanceof AbstractData))
			return false;

		var other = (AbstractData) o;

		final double EPS = 1e-8;

		if (abs(count - (Integer) other.getNumPoints().getValue()) > EPS)
			return false;

		if (signal.hashCode() != other.signal.hashCode())
			return false;

		if (time.hashCode() != other.time.hashCode())
			return false;

		return time.containsAll(other.time) && signal.containsAll(other.signal);

	}
	
}