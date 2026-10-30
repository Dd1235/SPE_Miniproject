package in.ac.iiitb.spe.calculator;

/**
 * Thrown when an operand has too many digits or too large an exponent (e.g. {@code 1e999999999}).
 * Such inputs can make BigDecimal arithmetic allocate billions of digits, so they are rejected up front.
 */
public class OperandOutOfRangeException extends RuntimeException {

	private final String parameter;

	public OperandOutOfRangeException(String parameter) {
		super("Parameter '" + parameter + "' is outside the supported range");
		this.parameter = parameter;
	}

	public String getParameter() {
		return parameter;
	}

}
