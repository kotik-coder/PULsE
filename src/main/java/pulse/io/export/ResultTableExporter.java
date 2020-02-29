package pulse.io.export;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.List;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.tasks.AbstractResult;
import pulse.tasks.AverageResult;
import pulse.ui.Messages;
import pulse.ui.components.ResultTable;
import pulse.ui.components.models.ResultTableModel;

public class ResultTableExporter implements Exporter<ResultTable> {

	private static ResultTableExporter instance = new ResultTableExporter();
	
	private ResultTableExporter() {
		
	}
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] {Extension.HTML, Extension.CSV};
	}
	
	@Override
	public void printToStream(ResultTable table, FileOutputStream fos, Extension extension) {
		switch(extension) {
			case HTML : printHTML(table, fos); break;
			case CSV : printCSV(table, fos); break;
			default : System.err.println("Extension not supported: " + extension);
		}		
	}
	
	/*
	 * PRINTING METHODS
	 */
	
	private void printCSV(ResultTable table, FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
		NumericPropertyKeyword p = null;
		
        for (int col = 0; col < table.getColumnCount(); col++) {
        	p = ( (ResultTableModel)table.getModel() ).getFormat().
        			fromAbbreviation(table.getColumnName(col));
            stream.print(p + "\t");
            stream.print("STD. DEV" + "\t");
        }
        
        stream.println(""); 

        NumericProperty tmp;
        String output;
        
        for (int i = 0; i < table.getRowCount(); i++) {
            for (int j = 0; j < table.getColumnCount(); j++) {
            	tmp = (NumericProperty) table.getValueAt(i,j);
            	output = tmp.formattedValue().replaceAll("[^a-zA-Z0-9.E+-]", "\t");
                stream.print(output + "\t");
                if(tmp.getError() == null)                	
                	stream.print("0.0\t");
            }
            stream.println();
        }        
        
        List<AbstractResult> results = ( (ResultTableModel)table.getModel() ).getResults();
        
        boolean printMore = false;
        
        for(AbstractResult ar : results) {
        	if(ar instanceof AverageResult)
        		printMore = true;
        }
        
        if(! printMore) {
        	stream.close();
        	return;
        }
        
        stream.println("");
        stream.print(Messages.getString("ResultTable.SeparatorCSV"));
        stream.println("");
        
        for (int col = 0; col < table.getColumnCount(); col++) {
        	p = ( (ResultTableModel)table.getModel() ).getFormat().
        			fromAbbreviation(table.getColumnName(col));
            stream.print(p + "\t");
            stream.print("STD. DEV" + "\t");
        }              

        stream.println();

        List<AbstractResult> ir;
        List<NumericProperty> props;
        
        for(AbstractResult r : results) {
        	if(r instanceof AverageResult) {
        		ir = ((AverageResult) r).getIndividualResults();
        		
        		for(AbstractResult aar : ir) {		
        			props = AbstractResult.filterProperties(aar);
        			
        			for (int j = 0; j < table.getColumnCount(); j++) {
                    	output = props.get(j).formattedValue().replaceAll("[^a-zA-Z0-9.E+-]", "\t");
                        stream.print(output + "\t");
                        if(props.get(j).getError() == null)                	
                        	stream.print("0.0\t");
        			}
        			
        			stream.println();
                            			
        		}
        		
        	}
        }
               
        stream.close();
	}
	
	private void printHTML(ResultTable table, FileOutputStream fos) {		
		PrintStream stream = new PrintStream(fos);
		
		stream.print("<table>"); 
		stream.print("<tr>"); 
		
        for (int col = 0; col < table.getColumnCount(); col++) {
        	stream.print("<td>"); 
            stream.print(table.getColumnName(col) + "\t"); 
            stream.print("</td>"); 
        }
        
        stream.print("</tr>"); 

        stream.println(""); 

        NumericProperty tmp;

        for (int i = 0; i < table.getRowCount(); i++) {
        	stream.print("<tr>"); 
            for (int j = 0; j < table.getColumnCount(); j++) {
            	stream.print("<td>"); 
            	tmp = (NumericProperty) table.getValueAt(i,j);
                stream.print(tmp.formattedValue());
                stream.print("</td>"); 
            }
            stream.println("</tr>"); 
        }
        
        stream.print("</table>"); 
        
        List<AbstractResult> results = ( (ResultTableModel)table.getModel() ).getResults();
        
        boolean printMore = false;
        
        for(AbstractResult ar : results) {
        	if(ar instanceof AverageResult)
        		printMore = true;
        }
        
        if(! printMore) {
        	stream.close();
        	return;
        }
        
        stream.print(Messages.getString("ResultTable.IndividualResults")); 
        stream.print("<table>"); 
        stream.print("<tr>"); 
        
        for (int col = 0; col < table.getColumnCount(); col++) {
        	stream.print("<td>"); 
            stream.print(table.getColumnName(col) + "\t"); 
            stream.print("</td>"); 
        }
        
        stream.print("</tr>"); 

        stream.println(""); 

        List<AbstractResult> ir;
        List<NumericProperty> props;
        
        for(AbstractResult r : results) {
        	if(r instanceof AverageResult) {
        		ir = ( (AverageResult) r ).getIndividualResults();
        		
        		for(AbstractResult aar : ir) {		
        			stream.print("<tr>"); 
        			props = AbstractResult.filterProperties( aar );
        			
        			for (int j = 0; j < table.getColumnCount(); j++) {
                    	stream.print("<td>"); 
                        stream.print(props.get(j).formattedValue());
                        stream.print("</td>"); 
                    }
        			
        			stream.print("</tr>"); 
        		}
        		
        	}
        }
                
        stream.print("</table>"); 
               
        stream.close();
	}

	public static ResultTableExporter getInstance() {
		return instance;
	}

	@Override
	public Class<ResultTable> target() {
		return ResultTable.class;
	}
}
