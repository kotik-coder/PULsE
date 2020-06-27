package pulse.tasks;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.listeners.ResultFormatListener;

/**
 * <p>
 * A singleton {@code ResultFormat}, which contains a list of
 * {@code NumericPropertyKeyword}s used for identification of
 * {@code NumericPropert}ies. The format is constructed using a string of unique
 * characters.
 * </p>
 */

public class ResultFormat {

	private List<NumericPropertyKeyword> nameMap;

	private final static NumericPropertyKeyword[] minimalArray = 
			new NumericPropertyKeyword[] { NumericPropertyKeyword.IDENTIFIER, NumericPropertyKeyword.TEST_TEMPERATURE, NumericPropertyKeyword.DIFFUSIVITY }; 
	
	/**
	 * <p>
	 * The default format specified by the
	 * {@code Messages.getString("ResultFormat.DefaultFormat")}. See file
	 * messages.properties in {@code pulse.ui}.
	 * </p>
	 */

	private static ResultFormat format = new ResultFormat();
	private static List<ResultFormatListener> listeners = new ArrayList<ResultFormatListener>();

	private ResultFormat() {
		this(Arrays.asList(minimalArray));
	}
	
	private ResultFormat(List<NumericPropertyKeyword> keys) {
		nameMap = new ArrayList<NumericPropertyKeyword>();
		for(NumericPropertyKeyword key : keys)
			nameMap.add(key);
	}

	private ResultFormat(ResultFormat fmt) {
		nameMap = new ArrayList<NumericPropertyKeyword>(fmt.nameMap.size());
		nameMap.addAll(fmt.nameMap);
	}

	public static void addResultFormatListener(ResultFormatListener rfl) {
		listeners.add(rfl);
	}

	public static ResultFormat generateFormat(List<NumericPropertyKeyword> keys) {
		format = new ResultFormat(keys);

		ResultFormatEvent rfe = new ResultFormatEvent(format);

		for (ResultFormatListener rfl : listeners)
			rfl.resultFormatChanged(rfe);

		return format;
	}

	/**
	 * This class uses a singleton pattern, meaning there is only instance of this
	 * class.
	 * 
	 * @return the single (static) instance of this class
	 */

	public static ResultFormat getInstance() {
		return format;
	}

	/**
	 * Retrieves the list of keyword associated with this {@code ResultFormat}
	 * 
	 * @return a list of keywords that can be used to access {@code NumericProperty}
	 *         objects
	 */

	public List<NumericPropertyKeyword> getKeywords() {
		return nameMap;
	}

	/**
	 * Creates a {@code List<String>} of default abbreviations corresponding to the
	 * list of keywords specific to {@code NumericProperty} objects.
	 * 
	 * @return a list of abbreviations (typically, for filling the result table
	 *         headers)
	 */

	public List<String> abbreviations() {
		return nameMap.stream().map(keyword -> NumericProperty.theDefault(keyword).getAbbreviation(true))
				.collect(Collectors.toList());
	}

	/**
	 * Creates a {@code List<String>} of default descriptions corresponding to the
	 * list of keywords specific to {@code NumericProperty} objects.
	 * 
	 * @return a list of abbreviations (typically, for filling the result table
	 *         tooltips)
	 */

	public List<String> descriptors() {
		return nameMap.stream().map(keyword -> NumericProperty.theDefault(keyword).getDescriptor(false))
				.collect(Collectors.toList());
	}

	/**
	 * Finds a {@code NumericPropertyKeyword} contained in the {@code nameMap}, the
	 * description of which matches {@code descriptor}.
	 * 
	 * @param descriptor a {@code String} describing the
	 *                   {@code NumericPropertyKeyword}
	 * @return the {@code NumericPropertyKeyword} object
	 */

	public NumericPropertyKeyword fromAbbreviation(String descriptor) {
		return nameMap.stream()
				.filter(keyword -> NumericProperty.theDefault(keyword).getAbbreviation(true).equals(descriptor))
				.findFirst().get();
	}
	
	/**
	 * Calculates the length of the format string, which is the same as the size of
	 * the keyword list.
	 * 
	 * @return an integer, representing the size of the format string.
	 */

	public int size() {
		return nameMap.size();
	}

	public int indexOf(NumericPropertyKeyword key) {
		if (nameMap.contains(key))
			return nameMap.indexOf(key);
		return -1;
	}

	public static NumericPropertyKeyword[] getMinimalArray() {
		return minimalArray;
	}

}