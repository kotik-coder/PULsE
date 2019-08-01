package pulse.tasks;

import pulse.properties.NumericProperty;

public abstract class AbstractResult {

	protected NumericProperty[] properties;
	protected ResultFormat format;
	protected AbstractResult parent;
	
	public AbstractResult(ResultFormat format) {
		this.format = format;
		properties = new NumericProperty[format.labels().length];
	}
	
	public AbstractResult getParent() {
		return parent;
	}
	
	public NumericProperty[] properties() {
		
		String[] names = format.shortNames();
		NumericProperty[] output = new NumericProperty[names.length];
		
		int i = 0;
		
		outer : for(NumericProperty property : properties) {
			
			for(String name : names) {
				if(name.equals(property.getSimpleName())) {
					output[i++] = property;
					continue outer;
				}	
			}
			
		}
				
		return output;
		
	}

	public ResultFormat getFormat() {
		return format;
	}
	
	public void setFormat(ResultFormat format) {
		this.format = format;
	}
	
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for(NumericProperty p : properties) {
			if(p == null)
				continue;
			sb.append(p.toString());
			sb.append(System.lineSeparator());
		}
		return sb.toString();
	}

	
}
