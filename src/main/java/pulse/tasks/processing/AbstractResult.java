package pulse.tasks.processing;

import static pulse.properties.NumericProperties.def;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.util.UpwardsNavigable;

/**
 * An {@code AbstractResult} is either an individual, independent
 * {@code Result}, a {@code Result} that forms a part of the
 * {@code AverageResult}, or the {@code AverageResult}, which combines other
 * {@code Result}s. It is specified by the {@code ResultFormat} and a list of
 * {@code NumericPropert}ies.
 *
 */

public abstract class AbstractResult extends UpwardsNavigable {

	private List<NumericProperty> properties;
	private ResultFormat format;

	/**
	 * Constructs an {@code AbstractResult} with the list of properties specified by
	 * {@code format}.
	 * 
	 * @param format a {@code ResultFormat}
	 */

	public AbstractResult(ResultFormat format) {
		this.format = format;
		properties = new ArrayList<>(format.size());
	}
	
	public AbstractResult(AbstractResult r) {
		this.properties = new ArrayList<>(r.getProperties());
		this.format = r.format;
	}

	public ResultFormat getFormat() {
		return format;
	}

	/**
	 * This will print out all the properties according to the {@code ResultFormat}.
	 */

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (NumericProperty p : properties) {
			if (p != null) {
				sb.append(p.toString());
				sb.append(System.lineSeparator());
			}
		}
		return sb.toString();
	}

	/**
	 * Returns a list of {@code NumericPropert}ies, which conform to the chosen
	 * {@code ResultFormat}
	 * 
	 * @return a list of relevant {@code NumericProperty} objects
	 */

	public List<NumericProperty> getProperties() {
		return properties;
	}

	protected void addProperty(NumericProperty p) {
		properties.add(p);
	}

	protected NumericProperty getProperty(int i) {
		return filterProperties(this, format).get(i);
	}

	public void setFormat(ResultFormat format) {
		this.format = format;
	}

	/**
	 * A static method for filtering the properties contained in the {@code result}
	 * to choose only those that conform to the {@code format}.
	 * 
	 * @param result an {@code AbstractResult} with a list of properties
	 * @param format the format used for filtering
	 * @return the filtered list of properties
	 */

	public static List<NumericProperty> filterProperties(AbstractResult result, ResultFormat format) {
		return format.getKeywords().stream().map(keyword -> {
			var p = result.properties.stream().filter(property -> property.getType().equals(keyword)).findFirst();
			return p.isPresent() ? p.get() : def(keyword);
		}).collect(Collectors.toList());
	}

	/**
	 * A static method for filtering the properties contained in the {@code result}
	 * to choose only those that conform to its {@code format}.
	 * 
	 * @param result an {@code AbstractResult} with a list of properties and a
	 *               specified format
	 * @return the filtered list of properties
	 */

	public static List<NumericProperty> filterProperties(AbstractResult result) {
		return filterProperties(result, result.format);
	}
	
	@Override
	public boolean equals(Object o) {
		if(o == this)
			return true;
		
		if(o == null)
			return false;
		
		if(! (o.getClass().equals(o.getClass())))
			return false;
		
		var another = (AbstractResult)o;
		
		if(!another.properties.containsAll(this.properties) || !this.properties.containsAll(another.properties))
			return false;
		
		return another.format.equals(this.format);
		
	}

}