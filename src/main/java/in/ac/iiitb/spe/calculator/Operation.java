package in.ac.iiitb.spe.calculator;

import java.util.Locale;

/** The four operations the API exposes. */
public enum Operation {

	ADD, SUBTRACT, MULTIPLY, DIVIDE;

	/** Lower-case name used in the JSON response, e.g. {@code "add"}. */
	public String key() {
		return name().toLowerCase(Locale.ROOT);
	}

}
