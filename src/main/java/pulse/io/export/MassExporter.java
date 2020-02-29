package pulse.io.export;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import pulse.util.Group;

public class MassExporter {	
	
	private MassExporter() {
		
	}
	
	/**
	 * <p>Recursively analyses all {@code Accessible}s that this object owns
	 * (including only children) and chooses those that are {@code Saveable}.</p>
	 * @return a full list of {@code Saveable}s.
	 */
	
	public static List<Group> contents(Group root) {
		var contents = new ArrayList<Group>();		

	    try {
	    	
			root.subgroups().stream().
			forEach(ph -> 						
				{
					/*
					 * Filter only children, not parents! 
					 */
						
				 if(root.getParent() != ph)  
					contents.add(ph);
				 
				 }
				
				);
			
			for(ListIterator<Group> it = contents.listIterator(); it.hasNext(); )
				MassExporter.contents(it.next()).stream().forEach(a -> it.add(a)); 
							
		} catch (IllegalArgumentException e) {
			System.err.println("Unable to generate saveable contents for " + root.getClass());
			e.printStackTrace();
		}

	    return contents;
	}
	
	public static void exportGroup(Group ac, File directory, Extension extension) {
		if(!directory.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + directory);
		
		File internalDirectory = new File(directory + File.separator + ac.describe() + File.separator); 
		internalDirectory.mkdirs();
				
		ExportManager.export(ac, directory, extension);
		contents(ac).stream().forEach(internalHolder -> {
				ExportManager.export(internalHolder, internalDirectory, extension);
		});
	}

}