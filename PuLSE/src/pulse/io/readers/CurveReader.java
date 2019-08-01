package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import pulse.input.ExperimentalData;

public interface CurveReader extends AbstractReader {
	
	public abstract ExperimentalData[] read(File file) throws IOException;	
	
	public static ExperimentalData[] sort(ExperimentalData[] array) {
		
		int[] ids = new int[array.length];
		
		for(int i = 0; i < array.length; i++) {
			
			ids[i] = array[i].getMetadata().getExternalID();
			
		}
		
		Arrays.sort(ids);
		
		List<ExperimentalData> sortedList = new ArrayList<ExperimentalData>();
		
		for(int id : ids) 
			for(ExperimentalData c : array) 
				if(c.getMetadata().getExternalID() == id)
					sortedList.add(c);
			
		return sortedList.toArray(new ExperimentalData[ids.length]);
		
	}
	
}