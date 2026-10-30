package in.ac.iiitb.spe.calculator;

/** Thrown when the divisor is zero; mapped to HTTP 400 by {@link ApiExceptionHandler}. */
public class DivisionByZeroException extends RuntimeException {

	public DivisionByZeroException() {
		super("Division by zero is undefined");
	}

}
