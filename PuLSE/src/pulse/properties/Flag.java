package pulse.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.problem.statements.Problem;

import static pulse.properties.NumericPropertyKeyword.*;

public class Flag implements Property {

	private NumericPropertyKeyword index;
	private boolean value;
	private String abbreviation;
	
	public Flag(NumericPropertyKeyword type) {
		this.index = type;
		value = false;
	}
	
	public Flag(NumericPropertyKeyword type, String abbreviation, boolean value) {
		this.index = type;
		this.abbreviation = abbreviation;
		this.value = value;
	}

	public NumericPropertyKeyword getType() {
		return index;
	}
	
	public Flag derive(boolean value) {
		return new Flag(this.index, this.abbreviation, value);
	}
	
	public Flag derive() {
		return derive(this.value);
	}
		
	@Override
	public String getDescriptor(boolean addHtmlTags) {
		return addHtmlTags ? "<html><b>Search for </b>" + abbreviation + "</html>" :
			"<b>Search for </b>" + abbreviation;
	}

	@Override
	public Object getValue() {
		return value;
	}

	public void setValue(Object value) {
		this.value = (boolean) value;
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + ": " + index.name();
	}
	
	public static List<NumericPropertyKeyword> convert(List<Flag> flags) {
		List<Flag> filtered = flags.stream().filter(flag -> (boolean) flag.getValue() ).collect(Collectors.toList());
		return filtered.stream().map(flag -> flag.getType()).collect(Collectors.toList());
	}
	
	public static List<Flag> defaultList() {
		List<Flag> flags = new ArrayList<Flag>();
		flags.add(new Flag(NumericPropertyKeyword.DIFFUSIVITY, NumericProperty.def(DIFFUSIVITY).getAbbreviation(true), true));
		flags.add(new Flag(NumericPropertyKeyword.HEAT_LOSS, NumericProperty.def(HEAT_LOSS).getAbbreviation(true), true));
		flags.add(new Flag(NumericPropertyKeyword.MAXTEMP, NumericProperty.def(MAXTEMP).getAbbreviation(true), true));
		flags.add(new Flag(NumericPropertyKeyword.BASELINE_INTERCEPT, NumericProperty.def(BASELINE_INTERCEPT).getAbbreviation(true), false));
		flags.add(new Flag(NumericPropertyKeyword.BASELINE_SLOPE, NumericProperty.def(BASELINE_SLOPE).getAbbreviation(true), false));
		return flags;
	}

	public String getAbbreviation(boolean addHtmlTags) {
		return addHtmlTags ? "<html>" + abbreviation + "</html>" : abbreviation;
	}

	public void setAbbreviation(String abbreviation) {
		this.abbreviation = abbreviation;
	}

}
