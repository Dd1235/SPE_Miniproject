package in.ac.iiitb.spe.calculator;

import java.math.BigDecimal;
import java.math.MathContext;

import org.springframework.stereotype.Service;

/**
 * The calculator's business logic, kept free of any HTTP concerns so it can be unit-tested directly.
 * <p>
 * Uses {@link BigDecimal} rather than {@code double} so decimal inputs are exact: 0.1 + 0.2 is 0.3, not
 * 0.30000000000000004.
 */
@Service
public class CalculatorService {

	/** Division results are rounded to 34 significant digits (IEEE 754 decimal128, HALF_EVEN). */
	static final MathContext DIVISION_CONTEXT = MathContext.DECIMAL128;

	/** Largest number of significant digits accepted for an operand. */
	static final int MAX_PRECISION = 100;

	/** Largest decimal exponent accepted for an operand, in either direction. */
	static final int MAX_SCALE = 1_000;

	public CalculationResult calculate(Operation operation, BigDecimal a, BigDecimal b) {
		requireSupported("a", a);
		requireSupported("b", b);
		BigDecimal result = switch (operation) {
			case ADD -> a.add(b);
			case SUBTRACT -> a.subtract(b);
			case MULTIPLY -> a.multiply(b);
			case DIVIDE -> divide(a, b);
		};
		return new CalculationResult(operation.key(), normalize(a), normalize(b), normalize(result));
	}

	private static BigDecimal divide(BigDecimal a, BigDecimal b) {
		if (b.signum() == 0) {
			throw new DivisionByZeroException();
		}
		return a.divide(b, DIVISION_CONTEXT);
	}

	private static void requireSupported(String parameter, BigDecimal value) {
		if (value.precision() > MAX_PRECISION || Math.abs(value.scale()) > MAX_SCALE) {
			throw new OperandOutOfRangeException(parameter);
		}
	}

	/** Drops trailing zeros without switching to exponent notation: 5.00 becomes 5, 1E+3 becomes 1000. */
	static BigDecimal normalize(BigDecimal value) {
		BigDecimal stripped = value.stripTrailingZeros();
		return stripped.scale() < 0 ? stripped.setScale(0) : stripped;
	}

}
