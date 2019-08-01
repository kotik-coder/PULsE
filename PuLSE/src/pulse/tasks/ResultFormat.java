package pulse.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import pulse.properties.NumericProperty;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.listeners.ResultFormatListener;

public class ResultFormat {
	
	private Map<String,String> nameMap;
	
	public static final ResultFormat defaultFormat = new ResultFormat(Messages.getString("ResultFormat.DefaultFormat")); //$NON-NLS-1$
	
	private static ResultFormat format = new ResultFormat(defaultFormat); 

	private static String formatString;
	
	private final static char[] allowedCharacters = {'D', 'S', 'T', 'B', 'M', 'R', 'C', 'E', 'Q', 'A'};
	private final static char[] minimumAllowed = {'T', 'D'};
	
	private static List<ResultFormatListener> listeners = new ArrayList<ResultFormatListener>();
	
	/* Codes:
	 * D - thermal diffusivity
	 * S - specific heat (constant volume)
	 * T - initial temperature)
	 * B - biot number (any)
	 * M - maximum heating
	 * R - density
	 * C - thermal conductivity
	 * E - integral emissivity
	 * Q - absorbed energy
	 * A - accuracy or approximation 
	 */
	
	private ResultFormat(String formatString) {
		ResultFormat.formatString = formatString;
		nameMap = new HashMap<String,String>();
		
		char[] charArray = formatString.toCharArray();
		
		for(char c : charArray) {
			
			switch (c) {
				case 'D' : add(NumericProperty.DEFAULT_DIFFUSIVITY.getSimpleName(), 
						       Messages.getString("ResultFormat.1")); //$NON-NLS-1$
						   break;
				case 'S' : add(NumericProperty.DEFAULT_CV.getSimpleName(), 
							   Messages.getString("ResultFormat.2")); //$NON-NLS-1$
						   break;
				case 'T' : add(NumericProperty.DEFAULT_T.getSimpleName(),
							   Messages.getString("ResultFormat.3")); //$NON-NLS-1$
						   break;
				case 'B' : add(NumericProperty.DEFAULT_BIOT.getSimpleName(),
							   Messages.getString("ResultFormat.4")); //$NON-NLS-1$
						   break;
				case 'M' : add(NumericProperty.DEFAULT_MAXTEMP.getSimpleName(),
							   Messages.getString("ResultFormat.5")); //$NON-NLS-1$
						   break;
				case 'R' : add(NumericProperty.DEFAULT_RHO.getSimpleName(), 
							   Messages.getString("ResultFormat.6")); //$NON-NLS-1$
						   break;
				case 'C' : add(Messages.getString("ResultFormat.7"), //$NON-NLS-1$
							   Messages.getString("ResultFormat.8")); //$NON-NLS-1$
						   break;
				case 'E' : add(Messages.getString("ResultFormat.9"), //$NON-NLS-1$
							   Messages.getString("ResultFormat.10")); //$NON-NLS-1$
						   break;
				case 'Q' : add(NumericProperty.DEFAULT_QABS.getSimpleName(),
							   Messages.getString("ResultFormat.11")); //$NON-NLS-1$
						   break;
				case 'A' : add(Messages.getString("ResultFormat.12"),  //$NON-NLS-1$
						       Messages.getString("ResultFormat.13")); //$NON-NLS-1$
						   break;
				default  : throw new IllegalArgumentException(Messages.getString("ResultFormat.UnknownFormatError") + c); //$NON-NLS-1$
			}
			
		}
		
	}

	private ResultFormat(ResultFormat fmt) {
		nameMap = new HashMap<String,String>();
		nameMap.putAll(fmt.nameMap);
	}
	
	public static void addResultFormatListener(ResultFormatListener rfl) {
		listeners.add(rfl);
	}
	
	public static ResultFormat generateFormat(String formatString) {
		format = new ResultFormat(formatString);
		
		ResultFormatEvent rfe = new ResultFormatEvent(format);
		
		for(ResultFormatListener rfl : listeners)
			rfl.resultFormatChanged( rfe );
		
		return format;
	}
	
	public static ResultFormat getFormat() {
		return format;
	}
	
	private void add(String shortName, String longName) {
		nameMap.put(shortName, longName);
	}
	
	public String[] shortNames() {
		return nameMap.keySet().toArray(new String[nameMap.size()]);
	}
	
	public String label(String shortName) {
		return nameMap.get(shortName);
	}
	
	public String[] labels() {
		List<String> label = new LinkedList<String>();
		for(String key : nameMap.keySet()) 
			label.add(nameMap.get(key));
		return label.toArray(new String[label.size()]);
	}
	
	@Override
	public String toString() {
		return formatString;
	}

	public static char[] getAllowedCharacters() {
		return allowedCharacters;
	}

	public static char[] getMinimumAllowedFormat() {
		return minimumAllowed;
	}
	
}
