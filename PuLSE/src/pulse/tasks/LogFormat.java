package pulse.tasks;

import java.util.LinkedList;
import java.util.List;

public class LogFormat {
	
	private List<String> names, labels;
	
	public static final LogFormat DEFAULT_FORMAT = new LogFormat(Messages.getString("LogFormat.2")); //$NON-NLS-1$
	
	private static LogFormat format = new LogFormat(DEFAULT_FORMAT);
	
	/* Codes:
	 * I - Iteration 
	 * D - current direction
	 * G - current gradient
	 * H - current hessian
	 * S - current sum of squares
	 * R - current R^2 
	 */
	
	private	LogFormat(String formatString) {
		
		names		= new LinkedList<String>();
		labels		= new LinkedList<String>();
		
		char[] charArray = formatString.toCharArray();
		
		for(char c : charArray) {
			
			switch (c) {
				case 'I' : add("Iteration", Messages.getString("LogFormat.1")); //$NON-NLS-1$ //$NON-NLS-2$
						   break;
				case 'D' : add("Direction", Messages.getString("LogFormat.0")); //$NON-NLS-1$ //$NON-NLS-2$
						   break;
				case 'H' : add("Hessian", Messages.getString("LogFormat.6")); //$NON-NLS-1$ //$NON-NLS-2$
						   break;
				case 'G' : add("Gradient", Messages.getString("LogFormat.8")); //$NON-NLS-1$ //$NON-NLS-2$
						   break;
				case 'S' : add("SumOfSquares", Messages.getString("LogFormat.10")); //$NON-NLS-1$ //$NON-NLS-2$
						   break;
				case 'R' : add("RSquared", Messages.getString("LogFormat.12")); //$NON-NLS-1$ //$NON-NLS-2$
						   break;
				default  : throw new IllegalArgumentException(Messages.getString("LogFormat.UnknownFormatError") + c); //$NON-NLS-1$
			}
			
		}
		
	}

	private LogFormat(LogFormat fmt) {
		this.labels		 = new LinkedList<String>(fmt.labels);
	}
	
	public static LogFormat generateFormat(String formatString) {
		format = new LogFormat(formatString);
		return format;
	}
	
	public static LogFormat getFormat() {
		return format;
	}
	
	private void add(String name, String label) {
		names.add(name);
		labels.add(label);
	}
	
	public String[] labels() {
		return labels.toArray(new String[labels.size()]);
	}
	
	public String[] names() {
		return names.toArray(new String[labels.size()]);
	}
	
}