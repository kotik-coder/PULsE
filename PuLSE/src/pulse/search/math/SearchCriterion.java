package pulse.search.math;

import pulse.properties.NumericProperty;

public enum SearchCriterion implements Index {
	R_SQUARED, SUM_OF_SQUARES;

	public static SearchCriterion valueOf(NumericProperty p) {
		switch(p.getSimpleName()) {
		case "RSquared" : 	return R_SQUARED;
		case "sumOfSquares" : return SUM_OF_SQUARES;
		}
		return null;
	}
	
}
