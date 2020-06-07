package pulse.tasks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pulse.properties.NumericPropertyKeyword;
import pulse.search.math.IndexedVector;
import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.EmptyCorrelationTest;
import pulse.util.ImmutableDataEntry;
import pulse.util.ImmutablePair;

public class CorrelationBuffer {

	private List<IndexedVector> params;
	private static Set<ImmutablePair<NumericPropertyKeyword>> excludePairList;
	private static Set<NumericPropertyKeyword> excludeSingleList;

	static {
		excludePairList = new HashSet<ImmutablePair<NumericPropertyKeyword>>();
		excludeSingleList = new HashSet<NumericPropertyKeyword>();
		excludeSingle(NumericPropertyKeyword.DIFFUSIVITY);
		excludePair(NumericPropertyKeyword.HEAT_LOSS, NumericPropertyKeyword.MAXTEMP);
		excludePair(NumericPropertyKeyword.MAXTEMP, NumericPropertyKeyword.BASELINE_INTERCEPT);
		excludePair(NumericPropertyKeyword.MAXTEMP, NumericPropertyKeyword.BASELINE_SLOPE);
		excludePair(NumericPropertyKeyword.MAXTEMP, NumericPropertyKeyword.DIATHERMIC_COEFFICIENT);
		excludePair(NumericPropertyKeyword.BASELINE_INTERCEPT, NumericPropertyKeyword.DIATHERMIC_COEFFICIENT);
	}

	public CorrelationBuffer() {
		params = new ArrayList<IndexedVector>();
	}

	public void inflate(SearchTask t) {
		params.add(t.searchVector()[0]);
	}

	public void clear() {
		params.clear();
	}

	public Map<ImmutablePair<NumericPropertyKeyword>, Double> evaluate(CorrelationTest t) {
		if (params.size() < 1)
			throw new IllegalStateException("Zero number of entries in parameter list");

		if (t instanceof EmptyCorrelationTest)
			return null;

		var indices = params.get(0).getIndices();
		var map = indices.stream()
				.map(index -> new ImmutableDataEntry<NumericPropertyKeyword, double[]>(index,
						params.stream().mapToDouble(v -> v.get(index)).toArray()))
				.collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));

		int indicesSize = indices.size();
		var correlationMap = new HashMap<ImmutablePair<NumericPropertyKeyword>, Double>();
		ImmutablePair<NumericPropertyKeyword> pair = null;

		for (int i = 0; i < indicesSize; i++) {

			if (!excludeSingleList.contains(indices.get(i)))
				for (int j = i + 1; j < indicesSize; j++) {
					pair = new ImmutablePair<NumericPropertyKeyword>(indices.get(i), indices.get(j));
					if (!excludeSingleList.contains(indices.get(j)) && !excludePairList.contains(pair))
						correlationMap.put(pair, t.evaluate(map.get(indices.get(i)), map.get(indices.get(j))));
				}

		}

		return correlationMap;

	}

	public boolean test(CorrelationTest t) {
		var map = evaluate(t);

		if (map == null)
			return false;

		for (Double d : map.values())
			if (t.compareToThreshold(d))
				return true;
		return false;
	}

	public static void excludePair(ImmutablePair<NumericPropertyKeyword> pair) {
		excludePairList.add(pair);
	}

	public static void excludePair(NumericPropertyKeyword first, NumericPropertyKeyword second) {
		excludePair(new ImmutablePair<NumericPropertyKeyword>(first, second));
	}

	public static void excludeSingle(NumericPropertyKeyword key) {
		excludeSingleList.add(key);
	}

}