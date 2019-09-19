package pulse.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.util.UpwardsNavigable;

/**
 * An {@code AbstractResult} is either an individual, independent {@code Result},
 * a {@code Result} that forms a part of the {@code AverageResult}, or the 
 * {@code AverageResult}, which combines other {@code Result}s. It is specified
 * by the {@code ResultFormat} and a list of {@code NumericPropert}ies.
 *
 */

public abstract class AbstractResult extends UpwardsNavigable {

	private List<NumericProperty> properties;
	private ResultFormat format;
	
	/**
	 * Constructs an {@code AbstractResult} with the list of properties
	 * specified by {@code format}.
	 * @param format a {@code ResultFormat} 
	 */
	
	public AbstractResult(ResultFormat format) {
		this.format = format;
		properties  = new ArrayList<NumericProperty>(format.length());
	}

	public ResultFormat getFormat() {
		return format;
	}
	
	public void setFormat(ResultFormat format) {
		this.format = format;
	}
	
	/**
	 * This will print out all the properties according to the {@code ResultFormat}.
	 */
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(NumericProperty p : properties) {
			if(p == null)
				continue;
			sb.append(p.toString());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}
	
	/**
	 * Returns a list of {@code NumericPropert}ies, which conform to the chosen
	 * {@code ResultFormat}
	 * @return a list of relevant {@code NumericProperty} objects
	 */

	public List<NumericProperty> getProperties() {
		return properties;
	}
	
	protected void addProperty(NumericProperty p) {
		properties.add(p);
	}
	
	protected NumericProperty getProperty(int i) {
		return properties.get(i);
	}

	/**
	 * A static method for filtering the properties contained in the {@code result} to choose only
	 * those that conform to the {@code format}. 
	 * @param result an {@code AbstractResult} with a list of properties
	 * @param format the format used for filtering
	 * @return the filtered list of properties
	 */
	
	public static List<NumericProperty> filterProperties(AbstractResult result, ResultFormat format) {
		return format.getKeywords().stream().map( keyword ->
			   		{ Optional<NumericProperty> p = result.properties.stream()
			   		  .filter(property-> property.getType().equals(keyword) )
			   		  .findFirst();
			   		  if(p.isPresent())
			   			  return p.get();
			   		  else
			   			  return NumericProperty.theDefault(keyword);
			   		} ).
						collect(Collectors.toList());
	}
	
	/**
	 * A static method for filtering the properties contained in the {@code result} to choose only
	 * those that conform to its {@code format}. 
	 * @param result an {@code AbstractResult} with a list of properties and a specified format
	 * @return the filtered list of properties
	 */
	
	public static List<NumericProperty> filterProperties(AbstractResult result) {
		return filterProperties(result, result.format);
	}

}