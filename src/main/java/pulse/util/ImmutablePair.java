package pulse.util;

public class ImmutablePair<T> {

	private T anElement;
	private T anotherElement;
	
	public ImmutablePair(T anElement, T anotherElement) {
		this.anElement = anElement;
		this.anotherElement = anotherElement;
	}

	public T getFirst() {
		return anElement;
	}

	public T getSecond() {
		return anotherElement;
	}
	
	@Override
	public boolean equals(Object o) {
		if(! (o instanceof ImmutablePair) )
			return false;
		
		if(this == o)
			return true;
		
		ImmutablePair<?> ip = (ImmutablePair<?>) o;
		
		if(this.getFirst().equals(ip.getFirst()))
			if(this.getSecond().equals(ip.getSecond()))
				return true;
		
		if(this.getFirst().equals(ip.getSecond()))
			if(this.getSecond().equals(ip.getFirst()))
				return true;
			
		return false;
		
		
	}
	
	@Override
	public int hashCode() {
	    return anElement.hashCode() + anotherElement.hashCode();
	}

}