package pulse.ui.components.models;

import static java.lang.Math.abs;
import static pulse.tasks.processing.AbstractResult.filterProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static javax.swing.SwingUtilities.invokeLater;

import javax.swing.table.DefaultTableModel;
import pulse.properties.NumericProperties;
import static pulse.properties.NumericPropertyKeyword.IDENTIFIER;
import static pulse.properties.NumericPropertyKeyword.TEST_TEMPERATURE;
import pulse.tasks.Calculation;

import pulse.tasks.Identifier;
import pulse.tasks.SearchTask;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.logs.Details;
import pulse.tasks.logs.Status;
import pulse.tasks.processing.AbstractResult;
import pulse.tasks.processing.AverageResult;
import pulse.tasks.processing.Result;
import pulse.tasks.processing.ResultFormat;
import pulse.ui.components.listeners.ResultListener;

@SuppressWarnings("serial")
public class ResultTableModel extends DefaultTableModel {

    private ResultFormat fmt;
    private List<AbstractResult> results;
    private List<String> tooltips;
    private List<ResultListener> listeners;

    public ResultTableModel(ResultFormat fmt, int rowCount) {
        super(fmt.abbreviations().toArray(), rowCount);
        this.fmt = fmt;
        results = new ArrayList<>();
        tooltips = tooltips();
        listeners = new ArrayList<>();
    }

    public ResultTableModel(ResultFormat fmt) {
        this(fmt, 0);
    }

    public void addListener(ResultListener listener) {
        listeners.add(listener);
    }

    public void removeListeners() {
        listeners.clear();
    }

    public void clear() {
        results.clear();
        listeners.clear();
        setRowCount(0);
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false; // all cells false
    }

    public void changeFormat(ResultFormat fmt) {
        this.fmt = fmt;

        for (var r : results) {
            r.setFormat(fmt);
        }

        if (this.getRowCount() > 0) {
            this.setRowCount(0);

            List<AbstractResult> oldResults = new ArrayList<>(results);

            results.clear();
            this.setColumnIdentifiers(fmt.abbreviations().toArray());

            oldResults.stream().filter(Objects::nonNull).forEach(r -> addRow(r));

        } else {
            this.setColumnIdentifiers(fmt.abbreviations().toArray());
        }

        tooltips = tooltips();

        listeners.stream().forEach(l -> l.onFormatChanged(new ResultFormatEvent(fmt)));

    }

    /**
     * Transforms the result model by merging individual results which: (a)
     * correspond to test temperatures within a specified
     * {@code temperatureDelta} (b) form a single sequence of measurements
     *
     * @param temperatureDelta the maximum difference between the test
     * temperature of two results being merged
     */
    public void merge(double temperatureDelta) {
        List<AbstractResult> skipList = new ArrayList<>();
        List<AbstractResult> avgResults = new ArrayList<>();
        List<AbstractResult> sortedResults = new ArrayList<>(results);

        /*sort results in the order of their ids
        * This is essential for the algorithm below which assumes the results
        * are listed in the order of ascending ids.
         */
        sortedResults.sort((AbstractResult arg0, AbstractResult arg1) -> {
            var id1 = arg0.getProperties().get(fmt.indexOf(IDENTIFIER));
            var id2 = arg1.getProperties().get(fmt.indexOf(IDENTIFIER));
            return NumericProperties.compare(id1, id2);
        });

        //iterated over the merged list
        for (var r : sortedResults) {

            //ignore results added to the skip list
            if (skipList.contains(r)) {
                continue;
            }

            //form a group of individual results corresponding to the specified criteria
            List<AbstractResult> group = group(sortedResults, r, temperatureDelta);
            //remove any previous occurences in that group
            group.removeAll(skipList);

            if (group.isEmpty()) {
                continue;
            } else if (group.size() == 1) {
                //just one result is being added - no need to average
                avgResults.addAll(group);
            } else {
                //add and average result
                avgResults.add(new AverageResult(group, fmt));
            }

            //ignore processed results later on
            skipList.addAll(group);

        }

        //populate model
        invokeLater(() -> {
            setRowCount(0);
            results.clear();
            avgResults.stream().filter(Objects::nonNull).forEach(r -> addRow(r));
        });

    }

    /**
     * Takes a list of results, which should be mandatory sorted in the order of
     * ascending id values, and searches for those results that can be merged
     * with {@code r}, satisfying these criteria: (a) these results correspond
     * to test temperatures within a specified {@code temperatureDelta} (b) they
     * form a single sequence of measurements
     *
     * @param listOfResults an orderer list of results, as explained above
     * @param r the result of interest
     * @param propertyInterval an interval for the temperature merging
     * @return a group of results
     */
    public List<AbstractResult> group(List<AbstractResult> listOfResults, AbstractResult r, double propertyInterval) {
        List<AbstractResult> selection = new ArrayList<>();

        final int idIndex = fmt.indexOf(IDENTIFIER);
        final int temperatureIndex = fmt.indexOf(TEST_TEMPERATURE);

        final double curTemp = ((Number) r.getProperties().get(temperatureIndex)
                .getValue()).doubleValue();

        final int curId = ((Number) r.getProperties().get(idIndex).getValue())
                .intValue();

        List<Integer> ids = new ArrayList<>();
        ids.add(curId);

        for (var rr : listOfResults) {

            var props = rr.getProperties();
            //temperature of a different result
            double temp = ((Number) props.get(temperatureIndex).getValue()).doubleValue();

            //if the property at modelIndex and the property value lie withing a specified interval
            if (abs(temp - curTemp) < propertyInterval) {

                //what is ID of that property?
                int newId = ((Number) props.get(idIndex).getValue()).intValue();

                //calculate the minimum "ID" distance between that property and 
                //the group elements, that should be either "one" or "zero"
                Optional<Integer> minDistance = ids.stream().map(id
                        -> (int) Math.abs(id - newId))
                        .reduce((a, b) -> a < b ? a : b);

                //accept only measurements within  a single interval
                if (minDistance.get() < 2) {
                    selection.add(rr);
                    ids.add(newId);
                }

            }

        }

        return selection;

    }

    private List<String> tooltips() {
        return fmt.descriptors();
    }

    public void addRow(AbstractResult result) {
        Objects.requireNonNull(result, "Entry added to the results table must not be null");

        //result must have a valid ancestor!
        var ancestor = Objects.requireNonNull(
                result.specificAncestor(SearchTask.class),
                "Result " + result.toString() + " does not belong a SearchTask!");

        //the ancestor then has the SearchTask type
        SearchTask parentTask = (SearchTask) ancestor;

        //any old result asssociated withis this task
        var oldResult = results.stream().filter(r
                -> r.specificAncestor(
                        SearchTask.class) == parentTask).findAny();

        //ignore average results
        if (result instanceof Result && oldResult.isPresent()) {
            AbstractResult oldResultExisting = oldResult.get();
            Optional<Calculation> oldCalculation = parentTask.getStoredCalculations().stream()
                    .filter(c -> c.getResult().equals(oldResultExisting)).findAny();

            //old calculation found
            if (oldCalculation.isPresent()) {

                //since the task has already been completed anyway
                Status status = Status.DONE;

                //better result than already present -- update table
                if (parentTask.getCurrentCalculation().isBetterThan(oldCalculation.get())) {
                    remove(oldResultExisting);
                    status.setDetails(Details.BETTER_CALCULATION_RESULTS_THAN_PREVIOUSLY_OBTAINED);
                    parentTask.setStatus(status);
                } else {
                    //do not remove result and do not add new result
                    status.setDetails(Details.CALCULATION_RESULTS_WORSE_THAN_PREVIOUSLY_OBTAINED);
                    parentTask.setStatus(status);
                    return;
                }

            } else {
            //calculation has been purged -- delete previous result

                remove(oldResultExisting);

            }

        }

        var propertyList = filterProperties(result, fmt);
        super.addRow(propertyList.toArray());
        results.add(result);

    }

    public void removeAll(Identifier id) {
        AbstractResult result = null;

        for (var i = results.size() - 1; i >= 0; i--) {
            result = results.get(i);

            if (!(result instanceof Result)) {
                continue;
            }

            if (id.equals(result.identify())) {
                results.remove(result);
                super.removeRow(i);
            }

        }

    }

    public void remove(AbstractResult r) {
        AbstractResult result = null;

        for (var i = results.size() - 1; i >= 0; i--) {
            result = results.get(i);

            if (result.equals(r)) {
                results.remove(result);
                super.removeRow(i);
            }

        }

    }

    public List<AbstractResult> getResults() {
        return results;
    }

    public ResultFormat getFormat() {
        return fmt;
    }

    public List<String> getTooltips() {
        return tooltips;
    }

}
