package pulse.util;

import java.util.ArrayList;
import java.util.List;

import pulse.tasks.Identifier;

public class Request {
	
	private Type type;
	private List<Identifier> idList;
	
	public Request(Type type, List<Identifier> ids) {
		this.type = type;
		this.idList = new ArrayList<Identifier>();
		for(Identifier id : ids) 
			idList.add(id);	
	}
	
	public Request(Type type, Identifier... ids) {
		this.type = type;
		this.idList = new ArrayList<Identifier>();
		for(Identifier id : ids) 
			idList.add(id);	
	}
	
	public Type getType() {
		return type;
	}
	
	public List<Identifier> getIdentifiers() {
		return idList;
	}
	
	public enum Type {
		CHART, LOAD;
	}

}