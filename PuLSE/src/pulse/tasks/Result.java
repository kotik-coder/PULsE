package pulse.tasks;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import pulse.properties.NumericProperty;
import pulse.properties.NumericPropertyKeyword;
import pulse.ui.Messages;
import pulse.util.Saveable;

/**
 * The individual {@code Result} that is associated with a {@code SearchTask}.
 * The {@code Identifier} of the task is stored as a field value.
 * @see pulse.tasks.SearchTask
 * @see pulse.tasks.Identifier
 */

public class Result extends AbstractResult implements Saveable {
	
	private Identifier identifier;
	
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
		
		this.identifier = task.getIdentifier();
		
		for(NumericPropertyKeyword name : format.getKeywords())
			try {
				addProperty( task.numericProperty(name) );
			} catch (IllegalAccessException | InvocationTargetException e) {
				System.out.println("Failed to use reflection when getting " + name + " from the SearchTask.");
			}											

	}	
	
	public Identifier getIdentifier() {
		return identifier;
	}

	/**
	 * Prints the data of this Result with {@code fos} in an html-format.
	 */
	
	@Override
	public void printData(FileOutputStream fos, Extension extension) {
		printHTML(fos);
	}
	
	private void printHTML(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
        
		stream.print("<table>"); 
        
        for (NumericProperty p : getProperties()) {
        	stream.print("<tr>");             
    		stream.print("<td>"); 
    		
            stream.print(
            		p.getType()
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
	
}