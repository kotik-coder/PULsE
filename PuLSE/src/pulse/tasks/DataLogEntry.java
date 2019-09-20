package pulse.tasks;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.ui.Messages;
import pulse.util.ImmutableDataEntry;

/**
 * <p>A {@code LogEntry} with a list of {@code NumericPropert}ies. Can be created
 * from a {@code SearchTask}. The output is accessible via the {@code toString()} method.</p>
 * 
 */

public class DataLogEntry extends LogEntry {

	private List<NumericProperty> entry; 
	
	/**
	 * Creates a new {@code DataLogEntry} based on the current values of the properties 
	 * from {@code task} which match the currently selected {@code LogFormat}. 
	 * @param task a task, which will be used to build the {@code DataLogEntry}
	 */
	
	public DataLogEntry(SearchTask task) {
		super(task);
		try {
			fill();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			System.err.println("Failed to fill this log entry with data. Details below.");
			e.printStackTrace();
		}							
	}	
	
	/**
	 * Fills this {@code DataLogEtnry} with properties from the {@code SearchTask},
	 * which have types matching to those listed in the {@code LogFormat}.
	 * @throws IllegalAccessException if the call to {@code task.numericProperties() fails}
	 * @throws IllegalArgumentException if the call to {@code task.numericProperties() fails}
	 * @throws InvocationTargetException if the call to {@code task.numericProperties() fails}
	 */
	
	private void fill() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		SearchTask task = TaskManager.getTask( getIdentifier() ); 
		entry = task.numericProperties().
				stream().filter(p -> 
					LogFormat.getInstance().types().stream().anyMatch(keyword -> p.getType() == keyword)
						).collect(Collectors.toList());
		Collections.sort(entry, (p1, p2) -> p1.getDescriptor(false).compareTo(p2.getDescriptor(false)));
		entry.add(0, task.getPath().getIteration());
	}
	
	public List<NumericProperty> getData() {
		return entry;
	}
	
	/**
	 * This {@code String} will be displayed by the {@code LogPane} if the 
	 * verbose log option is enabled.
	 * @see pulse.ui.components.LogPane
	 */
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();		
	
		sb.append("<table>");
		
		List<ImmutableDataEntry<String,String>> intermediate = 
				entry.stream().map(property -> new ImmutableDataEntry<String,String>(
				property.getDescriptor(true), property.formattedValue())).collect(Collectors.toList());
		
		for(ImmutableDataEntry<String,String> p : intermediate) {
			sb.append("<tr>");
			sb.append("<td>");
			sb.append(p.getKey());
			sb.append("</td>");
			sb.append("<td>");
			sb.append(Messages.getString("DataLogEntry.FontTagNumber")); //$NON-NLS-1$
			sb.append("<b>"); //$NON-NLS-1$
			sb.append(p.getValue());
			sb.append("</b>"); //$NON-NLS-1$
			sb.append(Messages.getString("DataLogEntry.FontTagClose")); //$NON-NLS-1$
			sb.append("</td>");
			sb.append("<br>"); //$NON-NLS-1$
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		
		sb.append("<br>"); //$NON-NLS-1$
		sb.append("<hr>"); //$NON-NLS-1$
		
		return sb.toString();
		
	}

}