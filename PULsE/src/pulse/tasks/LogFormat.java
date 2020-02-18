package pulse.tasks;

import java.util.ArrayList;
import java.util.List;

import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;

/**
 * The {@code LogFormat} determines what data should be visible in the {@code DataLogEntr}ies used for the {@code Log}.
 *
 */

public class LogFormat {
	
	private List<NumericPropertyKeyword> types;
	
	private static final LogFormat DEFAULT_FORMAT = new LogFormat(Messages.getString("LogFormat.2"));	
	private static LogFormat format = new LogFormat(DEFAULT_FORMAT);
	
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
						   add(NumericPropertyKeyword.DIATHERMIC_COEFFICIENT);
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
	
	/**
	 * <p>Generates a new {@code LogFormat} from the {@code formatString}. Only the following characters are 
	 * currently allowed (all characters must be unique): </p>
	 * <pre>
	 * O - search vector (contains multiple properties).
	 * S - current sum of squared residuals.
	 * R - current <i>R</i><sup>2</sup>.
	 * </pre>
	 * @param formatString the {@code} String containing some or all of these characters.
	 * @return a {@code LogFormat} constructed based on this {@code formatString}.
	 * @see pulse.tasks.ResultFormat.generateFormat(String)
	 */
	
	public static LogFormat generateFormat(String formatString) {
		format = new LogFormat(formatString);
		return format;
	}
	
	public static LogFormat getInstance() {
		return format;
	}
	
	private void add(NumericPropertyKeyword key) {
		types.add(key);
	}
	
	public List<NumericPropertyKeyword> types() {
		return types;
	}
	
}