package pulse.io.readers;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import pulse.input.ExperimentalData;

public interface CurveReader extends AbstractReader {
	
	public abstract List<ExperimentalData> read(File file) throws IOException;	
	
	public static List<ExperimentalData> sort(List<ExperimentalData> array) {
		Comparator<ExperimentalData> externalIdComparator = (ExperimentalData e1, ExperimentalData e2) -> 
			Integer.valueOf(e1.getMetadata().getExternalID()).compareTo(
					Integer.valueOf(e2.getMetadata().getExternalID()) );
			
		return array.stream().sorted(externalIdComparator).collect(Collectors.toList()); 		
	}
	
}