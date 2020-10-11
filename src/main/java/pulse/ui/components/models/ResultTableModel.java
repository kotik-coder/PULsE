package pulse.ui.components.models;

import static java.util.stream.Collectors.toList;
import static pulse.tasks.processing.AbstractResult.filterProperties;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.DefaultTableModel;

import pulse.tasks.Identifier;
import pulse.tasks.listeners.ResultFormatEvent;
import pulse.tasks.processing.AbstractResult;
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

			for (var r : oldResults) {
				addRow(r);
			}

		} else
			this.setColumnIdentifiers(fmt.abbreviations().toArray());

		tooltips = tooltips();

		listeners.stream().forEach(l -> l.onFormatChanged(new ResultFormatEvent(fmt)));

	}

	private List<String> tooltips() {
		return fmt.descriptors().stream().map(d -> "<html>" + d + "</html>").collect(toList());
	}

	public void addRow(AbstractResult result) {
		if (result == null)
			return;

		var propertyList = filterProperties(result, fmt);
		super.addRow(propertyList.toArray());
		results.add(result);

	}

	public void removeAll(Identifier id) {
		AbstractResult result = null;

		for (var i = results.size() - 1; i >= 0; i--) {
			result = results.get(i);

			if (!(result instanceof Result))
				continue;

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