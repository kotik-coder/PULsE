package pulse.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public interface Describable {

	public default String describe() {
		
		return getClass().getSimpleName() + "_" +
				DateTimeFormatter.ISO_DATE.format( LocalDateTime.now() );
		
	};
	
}
