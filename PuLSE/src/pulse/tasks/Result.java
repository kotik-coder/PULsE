package pulse.tasks;

import java.io.FileOutputStream;
import java.io.PrintStream;

import pulse.properties.NumericProperty;
import pulse.ui.Messages;
import pulse.util.Extension;
import pulse.util.Saveable;

/**
 * The individual {@code Result} that is associated with a {@code SearchTask}.
 * The {@code Identifier} of the task is stored as a field value.
 * @see pulse.tasks.SearchTask
 * @see pulse.tasks.Identifier
 */

public class Result extends AbstractResult implements Saveable {
	
	/**
	 * Creates an individual {@code Result} related to the current state of 
	 * {@code task} using the specified {@code format}.
	 * @param task a {@code SearchTask}, the properties of which that conform to {@code ResultFormat} will form this {@code Result}
	 * @param format a {@code ResultFormat}
	 * @throws IllegalArgumentException if {@code task} is null
	 */
	
	public Result(SearchTask task, ResultFormat format) throws IllegalArgumentException {
		super(format);
		
		if(task == null)
			throw new IllegalArgumentException(Messages.getString("Result.NullTaskError"));
		
		setParent(task);
		
		format.getKeywords().stream().forEach(key -> addProperty(task.numericProperty(key)));

	}	
	
	/**
	 * Prints the data of this Result with {@code fos} in an html-format.
	 */
	
	@Override
	public void printData(FileOutputStream fos, Extension extension) {
		switch(extension) {
			case HTML : printHTML(fos); break;
			case CSV : printCSV(fos); break;
	}
	}
	
	private void printHTML(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
        
		stream.print("<table>"); 
        
        for (NumericProperty p : getProperties()) {
        	stream.print("<tr>");             
    		stream.print("<td>"); 
    		
            stream.print(
            		p.getDescriptor(true)
            		); 
            stream.print("</td><td>"); 
            stream.print(
            		p.formattedValue(true)
            		); 
            
            stream.print("</td>");             
            stream.println("</tr>"); 
        }
        
		stream.print("</table>"); 
        
        stream.close();
	}
	
	/**
	 * The supported extensions for exporting the data contained in this object. Currently include {@code .html} and {@code .csv}.
	 */
	
	@Override
	public Extension[] getSupportedExtensions() {
		return new Extension[] {Extension.HTML, Extension.CSV};
	}
	
	private void printCSV(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
        stream.print("(Results)");
		
        for (NumericProperty p : getProperties()) {
        	stream.printf("%n%-20.10s", p.getType()); 
        	stream.printf("\t%-20.10s", p.formattedValue(true));   
        }
        stream.close();
	}
	
}