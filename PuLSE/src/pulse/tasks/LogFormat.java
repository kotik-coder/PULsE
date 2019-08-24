package pulse.tasks;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

public class LogFormat {
	
	private List<NumericPropertyKeyword> types;
	
	public static final LogFormat DEFAULT_FORMAT = new LogFormat(Messages.getString("LogFormat.2")); //$NON-NLS-1$
	
	private static LogFormat format = new LogFormat(DEFAULT_FORMAT);
	
	/* Codes:
	 * O - objective function
	 * S - current sum of squares
	 * R - current R^2 
	 */
	
	private	LogFormat(String formatString) {
		
		types		= new ArrayList<NumericPropertyKeyword>();
		
		char[] charArray = formatString.toCharArray();
		
		for(char c : charArray) {
			
			switch (c) {
				case 'O' : add(NumericPropertyKeyword.DIFFUSIVITY);
						   add(NumericPropertyKeyword.MAXTEMP);
						   add(NumericPropertyKeyword.HEAT_LOSS);
						   add(NumericPropertyKeyword.BASELINE_INTERCEPT);
						   add(NumericPropertyKeyword.BASELINE_SLOPE);
						   break;
				case 'S' : add(NumericPropertyKeyword.SUM_OF_SQUARES); 
						   break;
				case 'R' : add(NumericPropertyKeyword.RSQUARED);
						   break;
				default  : throw new IllegalArgumentException(Messages.getString("LogFormat.UnknownFormatError") + c); //$NON-NLS-1$
			}
			
		}
		
	}

	private LogFormat(LogFormat fmt) {
		this.types = new ArrayList<NumericPropertyKeyword>();
		this.types.addAll(fmt.types);
	}
	
	public static LogFormat generateFormat(String formatString) {
		format = new LogFormat(formatString);
		return format;
	}
	
	public static LogFormat getFormat() {
		return format;
	}
	
	private void add(NumericPropertyKeyword key) {
		types.add(key);
	}
	
	public List<NumericPropertyKeyword> types() {
		return types;
	}
	
}