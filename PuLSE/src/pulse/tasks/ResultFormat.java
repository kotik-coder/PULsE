package pulse.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.listeners.ResultFormatListener;
import pulse.ui.Messages;

import static pulse.properties.NumericPropertyKeyword.*;

/**
 * <p>A singleton {@code ResultFormat}, which contains a list of {@code NumericPropertyKeyword}s
 * used for identification of {@code NumericPropert}ies. The format is constructed using a 
 * string of unique characters.</p>  
 */

public class ResultFormat {
	
	private List<NumericPropertyKeyword> nameMap;
	
	private static String formatString;
	private final static char[] allowedCharacters	= 
		{'D', 'S', 'T', 'B', 'M', 'R', 'C', 'E', 'Q', 'A', 'U', 'V', 'I', 'N'};
	private final static char[] minimumAllowed		= 
		{'T', 'D'};
	
	/**
	 * <p>The default format specified by the {@code Messages.getString("ResultFormat.DefaultFormat")}.
	 * See file messages.properties in {@code pulse.ui}.</p>
	 */
	
	public static final ResultFormat DEFAULT_FORMAT = new ResultFormat(Messages.getString("ResultFormat.DefaultFormat"));
	
	private static ResultFormat format = new ResultFormat(DEFAULT_FORMAT); 	
	private static List<ResultFormatListener> listeners = new ArrayList<ResultFormatListener>();
				
	private ResultFormat(String formatString) {
		ResultFormat.formatString = formatString;
		nameMap = new ArrayList<NumericPropertyKeyword>();
		
		char[] charArray = formatString.toCharArray();
		
		for(char c : charArray) {
			
			switch (c) {
				case 'D' : nameMap.add(DIFFUSIVITY);
						   break;
				case 'S' : nameMap.add(SPECIFIC_HEAT);
						   break;
				case 'T' : nameMap.add(TEST_TEMPERATURE);
						   break;
				case 'B' : nameMap.add(HEAT_LOSS);
						   break;
				case 'M' : nameMap.add(MAXTEMP);
						   break;
				case 'R' : nameMap.add(DENSITY); 
						   break;
				case 'C' : nameMap.add(CONDUCTIVITY);
						   break;
				case 'E' : nameMap.add(EMISSIVITY);
						   break;
				case 'A' : nameMap.add(RSQUARED);
						   break;
				case 'U' : nameMap.add(BASELINE_INTERCEPT);
				   		   break;
				case 'V' : nameMap.add(BASELINE_SLOPE);
		   		   		   break;
				case 'N' : nameMap.add(DIATHERMIC_COEFFICIENT);
		   		   break;
				case 'I' : nameMap.add(IDENTIFIER);
		   		   break;
				default  : throw new IllegalArgumentException
				(Messages.getString("ResultFormat.UnknownFormatError") + c);
			}
			
		}
		
	}	

	private ResultFormat(ResultFormat fmt) {
		nameMap = new ArrayList<NumericPropertyKeyword>(fmt.nameMap.size());
		nameMap.addAll(fmt.nameMap);
	}
	
	public static void addResultFormatListener(ResultFormatListener rfl) {
		listeners.add(rfl);
	}
	
	/**
	 * Generates a new {@code ResultFormat} based on the {@code formatString} 
	 * and updates the single static instance of this class.
	 * This string should include unique valid characters from the following list.
	 * <pre>
	 * D - thermal diffusivity
	 * S - specific heat (constant volume)
	 * T - initial temperature)
	 * B - biot number (any)
	 * M - maximum heating
	 * R - density
	 * C - thermal conductivity
	 * E - integral emissivity
	 * Q - coefficient of absorption
	 * A - accuracy or approximation
	 * U - baseline intercept
	 * V - baseline slope 
	 * I - identifier</pre>
	 * After creating this new {@code ResultFormat}, a {@code ResultFormatEvent} object
	 * will be created and passed to the listeners via {@code resultFormatChanged(ResultFormatEvent)}.
	 * @param formatString a {@code String} specifying the new {@code ResultFormat}
	 * @return a new {@code ResultFormat}
	 * @see pulse.tasks.listeners.ResultFormatListener.resultFormatChange(ResultFormatEvent)
	 */
	
	public static ResultFormat generateFormat(String formatString) {
		format = new ResultFormat(formatString);
		
		ResultFormatEvent rfe = new ResultFormatEvent(format);

		for(ResultFormatListener rfl : listeners)
			rfl.resultFormatChanged( rfe );
		
		return format;
	}
	
	/**
	 * This class uses a singleton pattern, meaning there is only instance of this class.
	 * @return the single (static) instance of this class
	 */
	
	public static ResultFormat getInstance() {
		return format;
	}
	
	/**
	 * Retrieves the list of keyword associated with this {@code ResultFormat}
	 * @return a list of keywords that can be used to access {@code NumericProperty} objects
	 */
	
	public List<NumericPropertyKeyword> getKeywords() {
		return nameMap;
	}
	
	/**
	 * Creates a {@code List<String>} of default abbreviations corresponding
	 * to the list of keywords specific to {@code NumericProperty} objects.
	 * @return a list of abbreviations (typically, for filling the result table headers)
	 */
	
	public List<String> abbreviations() {
		return nameMap.stream().map(keyword -> 
		NumericProperty.theDefault(keyword).getAbbreviation(true)).
		collect(Collectors.toList());
	}
	
	/**
	 * Creates a {@code List<String>} of default descriptions corresponding
	 * to the list of keywords specific to {@code NumericProperty} objects.
	 * @return a list of abbreviations (typically, for filling the result table tooltips)
	 */
	
	public List<String> descriptors() {
		return nameMap.stream().map(keyword -> 
		NumericProperty.theDefault(keyword).getDescriptor(false)).
		collect(Collectors.toList());
	}
	
	/**
	 * Finds a {@code NumericPropertyKeyword} contained in the {@code nameMap}, 
	 * the description of which matches {@code descriptor}. 
	 * @param descriptor a {@code String} describing the {@code NumericPropertyKeyword}
	 * @return the {@code NumericPropertyKeyword} object
	 */
	
	public NumericPropertyKeyword fromAbbreviation(String descriptor) {
		return nameMap.stream().filter(keyword -> NumericProperty.theDefault(keyword).
				getAbbreviation(true).equals(descriptor)).findFirst().get();		
	}
	
	@Override
	public String toString() {
		return formatString;
	}
	
	/**
	 * Retrieves the default list of characters that are allowed in the 
	 * format string.
	 * @return a list of characters.
	 */

	public static char[] getAllowedCharacters() {
		return allowedCharacters;
	}
	
	/**
	 * Retrieves the default minimum of characters allowed in the format string.
	 * @return a minimum list of characters.
	 */

	public static char[] getMinimumAllowedFormat() {
		return minimumAllowed;
	}
	
	/**
	 * Calculates the length of the format string, which is the same as the 
	 * size of the keyword list.
	 * @return an integer, representing the size of the format string.
	 */
	
	public int length() {
		return formatString.length();
	}
	
	public int indexOf(NumericPropertyKeyword key) {
		if(nameMap.contains(key))
			return nameMap.indexOf(key);
		return -1;
	}
	
}