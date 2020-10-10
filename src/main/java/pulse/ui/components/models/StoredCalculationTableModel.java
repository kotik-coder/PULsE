package pulse.ui.components.models;

import static javax.swing.SwingUtilities.invokeLater;
import static pulse.properties.NumericProperties.def;
import static pulse.properties.NumericPropertyKeyword.MODEL_WEIGHT;

import javax.swing.table.DefaultTableModel;

import pulse.tasks.Calculation;
import pulse.tasks.SearchTask;

@SuppressWarnings("serial")
public class StoredCalculationTableModel extends DefaultTableModel {

	public static final int WEIGHT_COLUMN = 5;
	public static final int STATUS_COLUMN = 4;
	public static final int MODEL_STATISTIC_COLUMN = 3;
	public static final int OPTIMISER_STATISTIC_COLUMN = 2;
	public static final int BASELINE_COLUMN = 1;
	public static final int PROBLEM_COLUMN = 0;

	public StoredCalculationTableModel() {

		super(new Object[][] {},
				new String[] { "Problem Statement", "Baseline", "Parameter count",
						"Optimiser Statistic", "Model Selection Statistic",
						def(MODEL_WEIGHT).getAbbreviation(true) });
	}

	public void update(SearchTask t) {
		super.setRowCount(0);
		var list = t.getStoredCalculations();

		for(Calculation c : list) {
			var problem = c.getProblem();
			var baseline = c.getProblem().getBaseline();
			var optimiser = c.getOptimiserStatistic();
			var criterion = c.getModelSelectionCriterion();
			var parameters = c.getModelSelectionCriterion().getNumVariables();
			
			var weight = c.weight(list);

			var data = new Object[] { problem, baseline, parameters, optimiser.getStatistic(), criterion.getStatistic(), weight };

			invokeLater(() -> super.addRow(data));
		}

	}

}