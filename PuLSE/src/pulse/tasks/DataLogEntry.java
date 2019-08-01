package pulse.tasks;

import java.lang.reflect.InvocationTargetException;
import java.text.DecimalFormat;
import pulse.search.math.LogEntry;
import pulse.search.math.Vector;

public class DataLogEntry extends LogEntry {

	private Object[] values;
	
	private static final DecimalFormat bigNumber = new DecimalFormat(Messages.getString("DataLogEntry.BigNumberFormat")); //$NON-NLS-1$
	private static final DecimalFormat smallNumber = new DecimalFormat(Messages.getString("DataLogEntry.NumberFormat")); //$NON-NLS-1$
	private final static double smallDouble = 1e-3;
	private final static double largeDouble = 1e3;
	
	public DataLogEntry(SearchTask task) {
		super(task);
		fill();							
	}	
	
	public void fill() {

		SearchTask task = TaskManager.getTask( getIdentifier() );
		
		String[] shortNames = Log.getLogFormat().names();
		values 			    = new Object[shortNames.length];
		
		for(int i = 0; i < shortNames.length; i++) {
			Object byName = null;
			try {
				byName = task.value(shortNames[i]);
			} catch (NullPointerException | IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				System.err.println(Messages.getString("DataLogEntry.AccessError") + shortNames[i] + " for task " + task); //$NON-NLS-1$ //$NON-NLS-2$
				e.printStackTrace();
			}
			
			if(byName != null)  
				values[i] = byName;
			
			else 
				throw new IllegalArgumentException("Property " + shortNames[i] + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		}					
		
	}
	
	public Object[] getValues() {
		return values;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		
		final String[] labels = Log.getLogFormat().labels();
	
		sb.append("<br>"); //$NON-NLS-1$
		
		for(int i = 0; i < values.length; i++) {
			sb.append(labels[i]);
			sb.append(" : "); //$NON-NLS-1$
			sb.append("<b>"); //$NON-NLS-1$
			if(values[i] instanceof Double) {
				sb.append(Messages.getString("DataLogEntry.FontTagNumber")); //$NON-NLS-1$
				if(! Double.isFinite((double) values[i]))
					sb.append(sb);
				else if((double)values[i] > smallDouble && (double)values[i] < largeDouble)
						sb.append(smallNumber.format(values[i]));
				else 
						sb.append(bigNumber.format(values[i]));
				sb.append(Messages.getString("DataLogEntry.FontTagClose")); //$NON-NLS-1$
			} else {
				if(values[i] instanceof Vector)
					sb.append(Messages.getString("DataLogEntry.FontTagVector")); //$NON-NLS-1$
				sb.append(values[i]);
				sb.append(Messages.getString("DataLogEntry.FontTagClose")); //$NON-NLS-1$
			}
			sb.append("</b>"); //$NON-NLS-1$
			sb.append("<br>"); //$NON-NLS-1$
		}
		
		sb.append("<br>"); //$NON-NLS-1$
		sb.append("<hr>"); //$NON-NLS-1$
		
		return sb.toString();
		
	}

}