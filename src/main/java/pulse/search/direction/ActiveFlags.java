package pulse.search.direction;

import static pulse.properties.Flag.allProblemDependentFlags;
import static pulse.properties.Flag.allProblemIndependentFlags;
import static pulse.properties.Flag.selectActive;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import pulse.problem.statements.Problem;
import pulse.properties.Flag;
import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.properties.Property;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;

public class ActiveFlags {

	private static List<Flag> problemIndependentFlags = allProblemIndependentFlags();
	private static List<Flag> problemDependentFlags = allProblemDependentFlags();
	
	private ActiveFlags() {
		//empty constructor
	}
	
	public static void reset() {
		problemDependentFlags = allProblemDependentFlags();
		problemIndependentFlags = allProblemIndependentFlags();
	}
	
	public static List<Flag> getAllFlags() {
		var newList = new ArrayList<Flag>();
		newList.addAll(problemDependentFlags);
		newList.addAll(problemIndependentFlags);
		return newList;
	}
	
	public static void listAvailableProperties(List<Property> list) {
		list.addAll(problemIndependentFlags);

		var t = TaskManager.getManagerInstance().getSelectedTask();

		if (t != null) {
			var p = t.getCurrentCalculation().getProblem();

			if (p != null) {

				var params = p.listedTypes().stream().filter(pp -> pp instanceof NumericProperty)
						.map(pMap -> ((NumericProperty) pMap).getType()).collect(Collectors.toList());

				NumericPropertyKeyword key;

				for (Flag property : problemDependentFlags) {
					key = property.getType();
					if (params.contains(key))
						list.add(property);

				}

			}
		} else {
			for (Flag property : problemDependentFlags) {
				list.add(property);
			}
		}
	}
	
	/**
	 * Finds what properties are being altered in the search
	 * 
	 * @return a {@code List} of property types represented by
	 *         {@code NumericPropertyKeyword}s
	 */

	public static List<NumericPropertyKeyword> activeParameters(SearchTask t) {
		Problem p = t.getCurrentCalculation().getProblem();

		var list = new ArrayList<NumericPropertyKeyword>();
		list.addAll(selectActiveAndListed(problemDependentFlags, p));
		list.addAll(selectActiveTypes(problemIndependentFlags));
		return list;
	}
	
	public static List<NumericPropertyKeyword> selectActiveAndListed(List<Flag> flags, Problem listed) {
		return selectActiveTypes(flags).stream().filter(type -> listed.isListedNumericType(type))
				.collect(Collectors.toList());
	}

	public static List<NumericPropertyKeyword> selectActiveTypes(List<Flag> flags) {
		return selectActive(flags).stream().map(flag -> flag.getType()).collect(Collectors.toList());
	}

	public static List<Flag> getProblemIndependentFlags() {
		return problemIndependentFlags;
	}

	public static List<Flag> getProblemDependentFlags() {
		return problemDependentFlags;
	}
	
}