package pulse.util;

import java.io.File;
import java.util.List;

public interface SaveableCategory extends Describable {	

	public List<Saveable> contents();
	public default void saveCategory(File directory, Extension extension) {
		if(!directory.isDirectory())
			throw new IllegalArgumentException("Not a directory: " + directory);
		
		File internalDirectory = new File(directory + File.separator + this.describe() + File.separator); 
		internalDirectory.mkdirs();
		
		contents().stream().forEach(saveable -> {
			saveable.save(internalDirectory, extension);
		});
	}

}