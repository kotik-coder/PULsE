package pulse.tasks;

import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;

import pulse.properties.NumericProperty;
import pulse.properties.Property;
import pulse.util.Saveable;

public class Result extends AbstractResult implements Saveable {
	
	private Identifier identifier;
	
	public Result(SearchTask task, ResultFormat format) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		super(format);
		
		if(task == null)
			throw new IllegalArgumentException(Messages.getString("Result.NullTaskError")); //$NON-NLS-1$
			
		String[] shortNames = format.shortNames();
		
		this.identifier = task.getIdentifier();
		
		for(int i = 0; i < shortNames.length; i++) {
			Property byName = task.propertyByName(shortNames[i]);
			if(byName != null) 
				properties[i] = (NumericProperty)byName;
			else 
				throw new IllegalArgumentException("Property " + shortNames[i] + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
		}						

	}	
	
	@Override
	public NumericProperty[] properties() {
		NumericProperty[] nps = super.properties();
		SearchTask task = TaskManager.getTask(identifier);
		String[] shortNames = format.shortNames();
		
		for(int i = 0; i < nps.length; i++) {
			if(nps[i] == null) {
				Property byName = null;
				try {
					byName = task.propertyByName(shortNames[i]);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if(byName != null) 
					nps[i] = (NumericProperty)byName;
			}
				
		}
		
		return nps;
		
	}
	
	public Identifier getIdentifier() {
		return identifier;
	}

	@Override
	public void printData(FileOutputStream fos) {
		PrintStream stream = new PrintStream(fos);
	
        NumericProperty[] data = properties();
        
		stream.print("<table>"); //$NON-NLS-1$
        
        for (int i = 0; i < data.length; i++) {
        	stream.print("<tr>"); //$NON-NLS-1$            
    		stream.print("<td>"); //$NON-NLS-1$
    		
            stream.print(
            		getFormat().label(data[i].getSimpleName())
            		); //$NON-NLS-1$
            stream.print("</td><td>"); //$NON-NLS-1$
            stream.print(
            		data[i].formattedValue(true)
            		); //$NON-NLS-1$
            
            stream.print("</td>"); //$NON-NLS-1$            
            stream.println("</tr>"); //$NON-NLS-1$
        }
        
		stream.print("</table>"); //$NON-NLS-1$
        
        stream.close();
        
	}
	
}