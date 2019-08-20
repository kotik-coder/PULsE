package pulse.tasks;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.direction.PathSolver;
import pulse.util.DataEntry;

public class DataLogEntry extends LogEntry {

	private List<DataEntry<String,String>> intermediate;

	public DataLogEntry(SearchTask task) {
		super(task);
		fill();							
	}	
	
	public void fill() {		
		SearchTask task = TaskManager.getTask( getIdentifier() );
		
		LogFormat fmt = Log.getLogFormat();
		List<NumericPropertyKeyword> keywords = fmt.types();
		intermediate  = new ArrayList<DataEntry<String,String>>(keywords.size());

			keywords.stream().forEach(keyword -> 
			{
				try {
					NumericProperty num = task.numericProperty(keyword);
					if(!PathSolver.getSearchFlags().stream()
							.filter(flag -> flag.getType() == num.getType()).
							anyMatch(flag -> !(boolean)flag.getValue()))						
						intermediate.add(new DataEntry<String,String>
						(num.getAbbreviation().replace("<html>", "").replace("</html>", ""),
								num.formattedValue().replace("<html>", "").replace("</html>", "")));
				} catch (NullPointerException | IllegalArgumentException | IllegalAccessException | InvocationTargetException e) {
					System.err.println(Messages.getString("DataLogEntry.AccessError") + " " + keyword + " for task " + task); //$NON-NLS-1$ //$NON-NLS-2$
					e.printStackTrace();
				}
			});			
		
	}
	
	public List<DataEntry<String,String>> getValues() {
		return intermediate;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();		
	
		sb.append("<br>");
		sb.append("<table>");
		
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