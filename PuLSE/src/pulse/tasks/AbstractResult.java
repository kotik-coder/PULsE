package pulse.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.util.PropertyHolder;

public abstract class AbstractResult {

	private List<NumericProperty> properties;
	private ResultFormat format;
	private AbstractResult parent;
	
	public AbstractResult(ResultFormat format) {
		this.format = format;
		properties = new ArrayList<NumericProperty>(format.abbreviations().size());
	}
	
	public AbstractResult getParent() {
		return parent;
	}

	public ResultFormat getFormat() {
		return format;
	}
	
	public void setFormat(ResultFormat format) {
		this.format = format;
	}
	
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

	public List<NumericProperty> getProperties() {
		return properties;
	}
	
	public void addProperty(NumericProperty p) {
		properties.add(p);
	}
	
	public NumericProperty getProperty(int i) {
		return properties.get(i);
	}

	public void setParent(AbstractResult parent) {
		this.parent = parent;
	}
	
	public static List<NumericProperty> filterProperties(AbstractResult result, ResultFormat format) {
		return format.keywords().stream().map( keyword ->
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
	
	public static List<NumericProperty> filterProperties(AbstractResult result) {
		return filterProperties(result, result.format);
	}
	
	public static List<NumericProperty> relevantProperties(AbstractResult result, PropertyHolder holder) {
		List<NumericProperty> preliminary = AbstractResult.filterProperties(result);
		return preliminary.stream().filter(property ->
			holder.data().stream().filter(p -> p instanceof NumericProperty).
			anyMatch(taskProperty -> property.getType()
					.equals( ((NumericProperty)taskProperty).getType()) )).
				collect(Collectors.toList());
	}
	
}
