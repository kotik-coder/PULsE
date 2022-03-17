package pulse.tasks.processing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import pulse.math.ParameterVector;
import pulse.properties.NumericPropertyKeyword;
import pulse.search.statistics.CorrelationTest;
import pulse.search.statistics.EmptyCorrelationTest;
import pulse.tasks.SearchTask;
import pulse.util.ImmutableDataEntry;
import pulse.util.ImmutablePair;

public class CorrelationBuffer {

    private List<ParameterVector> params;
    private static Set<ImmutablePair<NumericPropertyKeyword>> excludePairList;
    private static Set<NumericPropertyKeyword> excludeSingleList;

    private final static double DEFAULT_THRESHOLD = 1E-3;
    
    static {
        excludePairList = new HashSet<>();
        excludeSingleList = new HashSet<>();
        excludeSingle(NumericPropertyKeyword.DIFFUSIVITY);
        excludePair(NumericPropertyKeyword.HEAT_LOSS_SIDE, NumericPropertyKeyword.MAXTEMP);
        excludePair(NumericPropertyKeyword.HEAT_LOSS, NumericPropertyKeyword.MAXTEMP);
        excludePair(NumericPropertyKeyword.MAXTEMP, NumericPropertyKeyword.BASELINE_INTERCEPT);
        excludePair(NumericPropertyKeyword.MAXTEMP, NumericPropertyKeyword.BASELINE_SLOPE);
        excludePair(NumericPropertyKeyword.MAXTEMP, NumericPropertyKeyword.HEAT_LOSS_COMBINED);
    }

    public CorrelationBuffer() {
        params = new ArrayList<>();
    }

    public void inflate(SearchTask t) {
        params.add(t.searchVector());
    }

    public void clear() {
        params.clear();
    }
    
    /**
     * Truncates the buffer by excluding nearly-converged results.
     */
    
    private void truncate(double threshold) {
        int i = 0;
        int size = params.size();
        final double thresholdSq = threshold*threshold;
        
        for(i = 0; i < size - 1; i = i + 2) {
            
            ParameterVector diff = new ParameterVector( params.get(i), params.get(i + 1).subtract(params.get(i) ));
            if(diff.lengthSq()/params.get(i).lengthSq() < thresholdSq)
                break;    
        }
        
        for(int j = size - 1; j > i; j--)
            params.remove(j);
       
    }

    public Map<ImmutablePair<NumericPropertyKeyword>, Double> evaluate(CorrelationTest t) {
        if (params.isEmpty()) {
            throw new IllegalStateException("Zero number of entries in parameter list");
        }

        if (t instanceof EmptyCorrelationTest) {
            return null;
        }

        truncate(DEFAULT_THRESHOLD);
        
        var indices = params.get(0).getIndices();
        var map = indices.stream()
                .map(index -> new ImmutableDataEntry<>(index, params.stream().mapToDouble(v -> v.getParameterValue(index)).toArray()))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));

        int indicesSize = indices.size();
        var correlationMap = new HashMap<ImmutablePair<NumericPropertyKeyword>, Double>();
        ImmutablePair<NumericPropertyKeyword> pair = null;

        for (int i = 0; i < indicesSize; i++) {

            if (!excludeSingleList.contains(indices.get(i))) {
                for (int j = i + 1; j < indicesSize; j++) {
                    pair = new ImmutablePair<>(indices.get(i), indices.get(j));
                    if (!excludeSingleList.contains(indices.get(j)) && !excludePairList.contains(pair)) {
                        correlationMap.put(pair, t.evaluate(map.get(indices.get(i)), map.get(indices.get(j))));
                    }
                }
            }

        }

        return correlationMap;

    }

    public boolean test(CorrelationTest t) {
        var map = evaluate(t);

        if (map == null) {
            return false;
        }

        return map.values().stream().anyMatch(d -> t.compareToThreshold(d));
    }

    public static void excludePair(ImmutablePair<NumericPropertyKeyword> pair) {
        excludePairList.add(pair);
    }

    public static void excludePair(NumericPropertyKeyword first, NumericPropertyKeyword second) {
        excludePair(new ImmutablePair<>(first, second));
    }

    public static void excludeSingle(NumericPropertyKeyword key) {
        excludeSingleList.add(key);
    }

}
