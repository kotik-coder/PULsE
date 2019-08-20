package pulse.util;

public class DataEntry<T,R> {
	public T key;
	public R value;
	
	public DataEntry(T key, R value) {
		this.key = key;
		this.value = value;
	}

	public T getKey() {
		return key;
	}

	public R getValue() {
		return value;
	}
	
	public String toString() {
		return "<" + key + " : " + value + ">";
	}

	public void setKey(T key) {
		this.key = key;
	}
			
}
