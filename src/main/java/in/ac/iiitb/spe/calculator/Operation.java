package in.ac.iiitb.spe.calculator;

import java.util.Locale;

/** The operations the API exposes. */
public enum Operation {

	ADD, SUBTRACT, MULTIPLY, DIVIDE, MODULO;

	/** Lower-case name used in the JSON response, e.g. {@code "add"}. */
	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

}
