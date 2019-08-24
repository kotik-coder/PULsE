package pulse.tasks;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.ui.Messages;
import pulse.util.DataEntry;

public class DataLogEntry extends LogEntry {

	private List<NumericProperty> entry; 
	private LogFormat fmt;
	
	public DataLogEntry(SearchTask task) {
		super(task);
		fmt = LogFormat.DEFAULT_FORMAT;
		try {
			fill();
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}							
	}	
	
	public void fill() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		SearchTask task = TaskManager.getTask( getIdentifier() ); 
		entry = task.numericProperties().
				stream().filter(p -> 
					fmt.types().stream().anyMatch(keyword -> p.getType() == keyword)
						).collect(Collectors.toList());
		Collections.sort(entry, (p1, p2) -> p1.getDescriptor(false).compareTo(p2.getDescriptor(false)));
		entry.add(0, task.getPath().getIteration());
	}
	
	public List<NumericProperty> getData() {
		return entry;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();		
	
		sb.append("<table>");
		
		List<DataEntry<String,String>> intermediate = 
				entry.stream().map(property -> new DataEntry<String,String>(
				property.getDescriptor(true), property.formattedValue())).collect(Collectors.toList());
		
		for(DataEntry<String,String> p : intermediate) {
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