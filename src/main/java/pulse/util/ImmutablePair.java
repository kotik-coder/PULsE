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
		if (!(o instanceof ImmutablePair))
			return false;

		if (this == o)
			return true;

		var ip = (ImmutablePair<?>) o;

		// direct order
		if (this.getFirst().equals(ip.getFirst()) && this.getSecond().equals(ip.getSecond()))
			return true;

		// reverse order
		return this.getFirst().equals(ip.getSecond()) && this.getSecond().equals(ip.getFirst());

	}

	@Override
	public int hashCode() {
		return anElement.hashCode() + anotherElement.hashCode();
	}

}