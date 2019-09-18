package pulse.ui.components;

import java.awt.Font;
import java.awt.event.MouseEvent;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.RowSorter;
import javax.swing.RowSorter.SortKey;
import javax.swing.SortOrder;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.AbstractResult;
import pulse.tasks.AverageResult;
import pulse.tasks.Identifier;
import pulse.tasks.Result;
import pulse.tasks.ResultFormat;
import pulse.tasks.SearchTask;
import pulse.tasks.TaskManager;
import pulse.tasks.listeners.TaskRepositoryEvent;
import pulse.tasks.listeners.TaskRepositoryListener;
import pulse.tasks.listeners.TaskSelectionEvent;
import pulse.tasks.listeners.TaskSelectionListener;
import pulse.ui.Messages;
import pulse.util.Saveable;

public class ResultTable extends JTable implements Saveable  {		
	
	private final static Font font = new Font(
			Messages.getString("ResultTable.FontName"), Font.PLAIN, 16);  //$NON-NLS-1$
	
	private final static int ROW_HEIGHT = 30;
	
	private NumericPropertyRenderer renderer;
	
	public ResultTable(ResultFormat fmt) {
		super();
		renderer = new NumericPropertyRenderer();		
		renderer.setVerticalAlignment( SwingConstants.TOP );
		
		ResultTableModel model = new ResultTableModel(fmt);
		setModel(model);
		setRowSorter(sorter());
		
		model.addListener(event -> setRowSorter(sorter()) );
		
		this.setRowHeight(ROW_HEIGHT);
		setShowHorizontalLines(false);
		setFillsViewportHeight(true);
		
		getTableHeader().setFont(font);
	    
		setSelectionMode(ListSelectionModel.SINGLE_INTERVAL_SELECTION);
	    setRowSelectionAllowed(false);
	    setColumnSelectionAllowed(true);
	    
	    /*
	     * Listen to TaskTable and select appropriate results when task selection changes
	     */
	    
	    TaskManager.addSelectionListener(new TaskSelectionListener() {

			@Override
			public void onSelectionChanged(TaskSelectionEvent e) {
				
				Identifier id = e.getSelection().getIdentifier();
				getSelectionModel().clearSelection();
				
				List<AbstractResult> results = ((ResultTableModel)getModel()).getResults();
				int jj = 0;
				
				for(AbstractResult r : results) {
					
					if(! (r instanceof Result) ) 
						continue;

					if(! ((Result)r).getIdentifier().equals(id) )
						continue;
						
					jj = convertRowIndexToView(results.indexOf(r));
						
					if(jj < -1)
						continue;
						
					getSelectionModel().addSelectionInterval(jj, jj);
					scrollToSelection(jj);					
					
				}
					
			}											
	    	
	    });
	    
	    /*
	     * Automatically add finished tasks to this result table
	     * Automatically remove results if corresponding task is removed
	     */
	    
		TaskManager.addTaskRepositoryListener(new TaskRepositoryListener() {

			@Override
			public void onTaskListChanged(TaskRepositoryEvent e) {
				
				switch(e.getState()) {
				
					case TASK_FINISHED :
						SearchTask t = TaskManager.getTask(e.getId());
						Result r = TaskManager.getResult(t);						
						SwingUtilities.invokeLater( () -> 
							((ResultTableModel)getModel()).addRow( r ));	
					break;
					case TASK_REMOVED :
					case TASK_RESET :
						((ResultTableModel)getModel()).removeAll( e.getId() );
						getSelectionModel().clearSelection();						
					break;	
				}				
				
				}
			
		});
	    
	}
	
	public void clear() {
		ResultTableModel model = (ResultTableModel) getModel();
		model.clear();
	}
	
	private TableRowSorter<ResultTableModel> sorter() {
		TableRowSorter<ResultTableModel> sorter = 
				new TableRowSorter<ResultTableModel>((ResultTableModel)getModel()); 
	    ArrayList<RowSorter.SortKey> list = new ArrayList<SortKey>();
		Comparator<NumericProperty> numericComparator = (i1, i2) -> i1.compareTo(i2);

	    for(int i = 0; i < getColumnCount(); i++) {
		    list.add( 
		    		new RowSorter.SortKey(i, SortOrder.ASCENDING) 
		    		);
		    sorter.setComparator(i, numericComparator);
	    }
	    
	    sorter.setSortKeys(list);
	    sorter.sort();
	    return sorter;
	}
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] {Saveable.Extension.HTML, Saveable.Extension.CSV};
	}
	
	public double[][][] data() {
		double[][][] data = new double[getColumnCount()][2][getRowCount()];
		NumericProperty property = null;
		
		for(int i = 0; i < data.length; i++) 
			for(int j = 0; j < data[0][0].length; j++) {
				property = (NumericProperty)getValueAt(j, i);
				data[i][0][j] = ((Number) property.getValue()).doubleValue() * ((Number) property.getDimensionFactor()).doubleValue();
				if(property.getError() != null)
					data[i][1][j] = property.getError().doubleValue() * 
									property.getDimensionFactor().doubleValue();
				else
					data[i][1][j] = 0;
			}	
		return data;
	}
	
	private void scrollToSelection(int rowIndex) {
		scrollRectToVisible(getCellRect(rowIndex, rowIndex, true));
	}
	
	@Override
	public TableCellRenderer getCellRenderer(int row, int column) {
		   Object value = getValueAt(row, column);
		   
		   if(value != null) 			   			   			   
			   if(value instanceof NumericProperty)
				   return renderer;	   
		   
		   return super.getCellRenderer(row, column);
		   
	}
	
	@Override
	public void printData(FileOutputStream fos, Extension extension) {
		switch(extension) {
			case HTML : printHTML(fos); break;
			case CSV : printCSV(fos); break;
		}		
	}
	
	/*
	 * Merges data withing a temperature interval
	 */
	
	public void merge(double temperatureDelta) {
		
		ResultTableModel model = (ResultTableModel) this.getModel();
		int temperatureIndex = model.getFormat().keywords().
				indexOf(NumericPropertyKeyword.TEST_TEMPERATURE);
		
		if(temperatureIndex < 0)
			return;

		Number val;
		
		List<Integer> indices;
		
		List<AbstractResult> newRows	= new LinkedList<AbstractResult>();
		List<Integer> skipList			= new ArrayList<Integer>();
		
		for(int i = 0; i < this.getRowCount(); i++) {
			if(skipList.contains(convertRowIndexToModel(i)))
				continue; //check if value is independent (does not belong to a group)
				
			val		= ((Number) (  (NumericProperty) this.
					getValueAt(i, temperatureIndex) ).getValue());
			
			indices	= group(val.doubleValue(), temperatureIndex, temperatureDelta); //get indices of results in table
			skipList.addAll(indices); //skip those indices if they refer to the same group

			if(indices.size() < 2) 
				newRows.add( model.getResults().get( indices.get(0) ) );
			else	
				newRows.add( new AverageResult(
						indices.stream().map(model.getResults()::get).
						collect(Collectors.toList())
						, model.getFormat() ) );
			
		}
		
		SwingUtilities.invokeLater( () -> 
			{				
				model.setRowCount(0);
				model.getResults().clear();
				
				for(AbstractResult row : newRows) 
					model.addRow( row );
				
			}			
		);
		
	}
	
	public List<Integer> group(double val, int index, double precision) {
		
		List<Integer> selection = new ArrayList<Integer>();
		Number valNumber;
		
		for(int i = 0; i < getRowCount(); i++) {
			
			valNumber = (Number) ((NumericProperty) getValueAt(i, index)).getValue();
			
			if( Math.abs( valNumber.doubleValue() - val) < precision)
				selection.add(convertRowIndexToModel(i));
			
		}
		
		return selection;
		
	}
	
	 //Implement table header tool tips.
	@Override
    protected JTableHeader createDefaultTableHeader() {
        return new JTableHeader(columnModel) {
            public String getToolTipText(MouseEvent e) {
                int index = columnModel.getColumnIndexAtX(e.getPoint().x);
                int realIndex = 
                        columnModel.getColumn(index).getModelIndex();
                return ((ResultTableModel)getModel()).getTooltips().get(realIndex);
            }
        };
	}
	
	/*
	 * PRINTING METHODS
	 */
	
	private void printCSV(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
		NumericProperty p = null;
		
        for (int col = 0; col < getColumnCount(); col++) {
        	p = ( (ResultTableModel)getModel() ).getFormat().
        			fromAbbreviation(getColumnName(col));
            stream.print(p.getType() + "\t");
            stream.print("STD. DEV" + "\t");
        }
        
        stream.println(""); 

        NumericProperty tmp;
        
        for (int i = 0; i < getRowCount(); i++) {
            for (int j = 0; j < getColumnCount(); j++) {
            	tmp = (NumericProperty) getValueAt(i,j);
                stream.print(tmp.formattedValue() + "\t");
                if(tmp.getError() != null)
                	stream.print(tmp.getError() + "\t");
                else
                	stream.print("0.0\t");
            }
            stream.println();
        }        
        
        List<AbstractResult> results = ( (ResultTableModel)getModel() ).getResults();
        
        boolean printMore = false;
        
        for(AbstractResult ar : results) {
        	if(ar instanceof AverageResult)
        		printMore = true;
        }
        
        if(! printMore) {
        	stream.close();
        	return;
        }
        
        stream.print(Messages.getString("ResultTable.IndividualResults")); //$NON-NLS-1$
        
        for (int col = 0; col < getColumnCount(); col++) 
            stream.print(getColumnName(col) + "\t");        

        stream.println(""); //$NON-NLS-1$

        List<AbstractResult> ir;
        AverageResult ar;
        Result rr;
        List<NumericProperty> props;
        
        for(AbstractResult r : results) {
        	if(r instanceof AverageResult) {
        		ar = (AverageResult) r;
        		ir = ar.getIndividualResults();
        		
        		for(AbstractResult aar : ir) {		
        			rr = (Result)aar;
        			props = AbstractResult.filterProperties(aar);
        			
        			for (int j = 0; j < getColumnCount(); j++) 
                        stream.print(props.get(j).formattedValue() + "\t");
        			
        			stream.println();
                            			
        		}
        		
        	}
        }
               
        stream.close();
	}
	
	private void printHTML(FileOutputStream fos) {		
		PrintStream stream = new PrintStream(fos);
		
		stream.print("<table>"); //$NON-NLS-1$
		stream.print("<tr>"); //$NON-NLS-1$
		
        for (int col = 0; col < getColumnCount(); col++) {
        	stream.print("<td>"); //$NON-NLS-1$
            stream.print(getColumnName(col) + "\t"); //$NON-NLS-1$
            stream.print("</td>"); //$NON-NLS-1$
        }
        
        stream.print("</tr>"); //$NON-NLS-1$

        stream.println(""); //$NON-NLS-1$

        NumericProperty tmp;

        for (int i = 0; i < getRowCount(); i++) {
        	stream.print("<tr>"); //$NON-NLS-1$
            for (int j = 0; j < getColumnCount(); j++) {
            	stream.print("<td>"); //$NON-NLS-1$
            	tmp = (NumericProperty) getValueAt(i,j);
                stream.print(tmp.formattedValue());
                stream.print("</td>"); //$NON-NLS-1$
            }
            stream.println("</tr>"); //$NON-NLS-1$
        }
        
        stream.print("</table>"); //$NON-NLS-1$
        
        List<AbstractResult> results = ( (ResultTableModel)getModel() ).getResults();
        
        boolean printMore = false;
        
        for(AbstractResult ar : results) {
        	if(ar instanceof AverageResult)
        		printMore = true;
        }
        
        if(! printMore) {
        	stream.close();
        	return;
        }
        
        stream.print(Messages.getString("ResultTable.IndividualResults")); //$NON-NLS-1$
        stream.print("<table>"); //$NON-NLS-1$
        stream.print("<tr>"); //$NON-NLS-1$
        
        for (int col = 0; col < getColumnCount(); col++) {
        	stream.print("<td>"); //$NON-NLS-1$
            stream.print(getColumnName(col) + "\t"); //$NON-NLS-1$
            stream.print("</td>"); //$NON-NLS-1$
        }
        
        stream.print("</tr>"); //$NON-NLS-1$

        stream.println(""); //$NON-NLS-1$

        List<AbstractResult> ir;
        AverageResult ar;
        Result rr;
        List<NumericProperty> props;
        
        for(AbstractResult r : results) {
        	if(r instanceof AverageResult) {
        		ar = (AverageResult) r;
        		ir = ar.getIndividualResults();
        		
        		for(AbstractResult aar : ir) {		
        			stream.print("<tr>"); //$NON-NLS-1$
        			rr = (Result)aar;
        			props = AbstractResult.filterProperties(aar);
        			
        			for (int j = 0; j < getColumnCount(); j++) {
                    	stream.print("<td>"); //$NON-NLS-1$
                        stream.print(props.get(j).formattedValue());
                        stream.print("</td>"); //$NON-NLS-1$
                    }
        			
        			stream.print("</tr>"); //$NON-NLS-1$
        		}
        		
        	}
        }
                
        stream.print("</table>"); //$NON-NLS-1$
               
        stream.close();
	}
	
}