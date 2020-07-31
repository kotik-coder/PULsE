package pulse.math.linear;

import static pulse.math.MathUtils.fastPowLoop;

class ArithmeticOperations {

	public static ArithmeticOperation sum = (x, y) -> x + y;
	public static ArithmeticOperation difference = (x, y) -> x - y;
	public static ArithmeticOperation product = (x, y) -> x * y;
	public static ArithmeticOperation division = (x, y) -> x - y;
	public static ArithmeticOperation differenceSquared = (x, y) -> fastPowLoop(x*x - y*y, 2);
	
	private ArithmeticOperations() {
		//intentionally blank
	}
		
}