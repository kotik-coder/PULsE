package pulse.tasks;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import pulse.properties.NumericProperty;
import pulse.ui.Messages;
import pulse.util.Saveable;

public class Result extends AbstractResult implements Saveable {
	
	private Identifier identifier;
	
	public Result(SearchTask task, ResultFormat format) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(format);
		
		if(task == null)
			throw new IllegalArgumentException(Messages.getString("Result.NullTaskError")); //$NON-NLS-1$
		
		this.identifier = task.getIdentifier();
		
		for(NumericProperty name : format.getNameList()) {
			NumericProperty current = task.numericProperty(name.getType());
			if(current != null) 
				addProperty((NumericProperty)current);
			else 
				throw new IllegalArgumentException("Property " + name.getType() + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		}						

	}	
	
	public Identifier getIdentifier() {
		return identifier;
	}

	@Override
	public void printData(FileOutputStream fos, Extension extension) {
		printHTML(fos);
	}
	
	private void printHTML(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
        
		stream.print("<table>"); //$NON-NLS-1$
        
        for (NumericProperty p : getProperties()) {
        	stream.print("<tr>"); //$NON-NLS-1$            
    		stream.print("<td>"); //$NON-NLS-1$
    		
            stream.print(
            		p.getType()
            		); //$NON-NLS-1$
            stream.print("</td><td>"); //$NON-NLS-1$
            stream.print(
            		p.formattedValue(true)
            		); //$NON-NLS-1$
            
            stream.print("</td>"); //$NON-NLS-1$            
            stream.println("</tr>"); //$NON-NLS-1$
        }
        
		stream.print("</table>"); //$NON-NLS-1$
        
        stream.close();
	}
	
}