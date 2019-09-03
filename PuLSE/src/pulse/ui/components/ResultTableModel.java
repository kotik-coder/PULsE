package pulse.ui.components;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import pulse.properties.NumericProperty;
import pulse.tasks.AbstractResult;
import pulse.tasks.Identifier;
import pulse.tasks.Result;
import pulse.tasks.ResultFormat;
import pulse.tasks.listeners.ResultFormatEvent;

public class ResultTableModel extends DefaultTableModel {

	private ResultFormat fmt;
	private List<AbstractResult> results;
	private List<String> tooltips;
	private List<ResultListener> listeners;
	
	public ResultTableModel(ResultFormat fmt, int rowCount) {
		super(fmt.abbreviations().toArray(), rowCount);
		this.fmt	= fmt;
		results		= new ArrayList<AbstractResult>();
	    tooltips	= tooltips();	
	    listeners	= new ArrayList<ResultListener>();
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
	
    @Override
    public boolean isCellEditable(int row, int column) {
       return false;        //all cells false
    }
    
    public void changeFormat(ResultFormat fmt) {
    	this.fmt = fmt;
    	
    	for(AbstractResult r : results)
    		r.setFormat(fmt);
    	
    	if(this.getRowCount() > 0) {    		    		    	    	
    		this.setRowCount(0);

    		List<AbstractResult> oldResults = new ArrayList<AbstractResult>(results);    	
    		
    		results.clear();    		    		
    		this.setColumnIdentifiers(fmt.abbreviations().toArray());
    	
    		for(AbstractResult r : oldResults)
    			addRow(r);
    	
    	} else 
    		this.setColumnIdentifiers(fmt.abbreviations().toArray());
    	
		tooltips = tooltips();
		
		listeners.stream().forEach(l -> l.onFormatChanged(new ResultFormatEvent(fmt)));
    	
    }
    
    private List<String> tooltips() {
    	return fmt.descriptors().stream().map(d -> "<html>" + d + "</html>").collect(Collectors.toList());
    }
    
	public void addRow(AbstractResult result) {
		if(result == null)
			return; 
		
		List<NumericProperty> propertyList = 
				AbstractResult.filterProperties(result, fmt);
		super.addRow(propertyList.toArray());
		results.add(result);
		
	}
	
	public void removeRow(Identifier id) {
		AbstractResult result = null;
		
		for(int i = results.size()-1; i >= 0; i--) {
			result = results.get(i);
			
			if(! (result instanceof Result))
				continue;
			
			if( ((Result)result).getIdentifier().equals(id) ) {
				results.remove(result);
				super.removeRow(i);
				break;
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