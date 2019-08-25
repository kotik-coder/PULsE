package pulse.tasks;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.problem.statements.Problem;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.listeners.ResultFormatListener;
import pulse.ui.Messages;

import static pulse.properties.NumericPropertyKeyword.*;

public class ResultFormat {
	
	private List<NumericProperty> nameMap;
	
	public static final ResultFormat defaultFormat = new ResultFormat(Messages.getString("ResultFormat.DefaultFormat")); //$NON-NLS-1$
	
	private static ResultFormat format = new ResultFormat(defaultFormat); 

	private static String formatString;
	
	private final static char[] allowedCharacters = {'D', 'S', 'T', 'B', 'M', 'R', 'C', 'E', 'Q', 'A', 'U', 'V', 'I'};
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
	 * U - baseline intercept
	 * V - baseline slope 
	 * I - identifier
	 */
	
	private ResultFormat(String formatString) {
		ResultFormat.formatString = formatString;
		nameMap = new ArrayList<NumericProperty>();
		
		char[] charArray = formatString.toCharArray();
		
		for(char c : charArray) {
			
			switch (c) {
				case 'D' : nameMap.add(NumericProperty.def(DIFFUSIVITY)); //$NON-NLS-1$
						   break;
				case 'S' : nameMap.add(NumericProperty.def(SPECIFIC_HEAT)); //$NON-NLS-1$
						   break;
				case 'T' : nameMap.add(NumericProperty.def(TEST_TEMPERATURE)); //$NON-NLS-1$
						   break;
				case 'B' : nameMap.add(NumericProperty.def(HEAT_LOSS)); //$NON-NLS-1$
						   break;
				case 'M' : nameMap.add(NumericProperty.def(MAXTEMP)); //$NON-NLS-1$
						   break;
				case 'R' : nameMap.add(NumericProperty.def(DENSITY)); //$NON-NLS-1$
						   break;
				case 'C' : nameMap.add(NumericProperty.def(CONDUCTIVITY));
						   break;
				case 'E' : nameMap.add(NumericProperty.def(EMISSIVITY));
						   break;
				case 'Q' : nameMap.add(NumericProperty.def(ABSORBED_ENERGY)); //$NON-NLS-1$
						   break;
				case 'A' : nameMap.add(NumericProperty.def(RSQUARED));
						   break;
				case 'U' : nameMap.add(NumericProperty.def(BASELINE_INTERCEPT));
				   		   break;
				case 'V' : nameMap.add(NumericProperty.def(BASELINE_SLOPE));
		   		   		   break;
				case 'I' : nameMap.add(Identifier.DEFAULT_IDENTIFIER);
		   		   break;
				default  : throw new IllegalArgumentException(Messages.getString("ResultFormat.UnknownFormatError") + c); //$NON-NLS-1$
			}
			
		}
		
	}

	private ResultFormat(ResultFormat fmt) {
		nameMap = new ArrayList<NumericProperty>(fmt.nameMap.size());
		nameMap.addAll(fmt.nameMap);
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
	
	public List<NumericPropertyKeyword> shortNames() {
		return nameMap.stream().map(property -> property.getType()).collect(Collectors.toList());
	}
	
	public List<String> abbreviations() {
		return nameMap.stream().map(property -> property.getAbbreviation(true)).collect(Collectors.toList());
	}
	
	public List<String> descriptors() {
		return nameMap.stream().map(property -> property.getDescriptor(false)).collect(Collectors.toList());
	}
	
	public NumericProperty fromAbbreviation(String descriptor) {
		for(NumericProperty p : nameMap)
			if(p.getAbbreviation(true).equals(descriptor))
				return p;
		return null;
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

	public List<NumericProperty> getNameMap() {
		return nameMap;
	}
	
}
