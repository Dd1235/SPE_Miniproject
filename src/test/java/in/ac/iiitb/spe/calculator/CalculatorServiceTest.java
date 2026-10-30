package in.ac.iiitb.spe.calculator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Unit tests for the business logic: plain JUnit, no Spring context, runs in milliseconds. */
class CalculatorServiceTest {

	private final CalculatorService calculator = new CalculatorService();

	private CalculationResult calc(Operation op, String a, String b) {
		return calculator.calculate(op, new BigDecimal(a), new BigDecimal(b));
	}

	@Test
	void addsTwoNumbers() {
		CalculationResult r = calc(Operation.ADD, "2", "3");
		assertThat(r.operation()).isEqualTo("add");
		assertThat(r.result()).isEqualByComparingTo("5");
	}

	@Test
	void subtractsTwoNumbers() {
		CalculationResult r = calc(Operation.SUBTRACT, "2", "3");
		assertThat(r.operation()).isEqualTo("subtract");
		assertThat(r.result()).isEqualByComparingTo("-1");
	}

	@Test
	void multipliesTwoNumbers() {
		CalculationResult r = calc(Operation.MULTIPLY, "6", "7");
		assertThat(r.operation()).isEqualTo("multiply");
		assertThat(r.result()).isEqualByComparingTo("42");
	}

	@Test
	void dividesTwoNumbers() {
		CalculationResult r = calc(Operation.DIVIDE, "10", "4");
		assertThat(r.operation()).isEqualTo("divide");
		assertThat(r.result()).isEqualByComparingTo("2.5");
	}

	@ParameterizedTest(name = "{0}({1}, {2}) = {3}")
	@CsvSource({
			"ADD,      0.1,  0.2,  0.3",  // exact decimals: double would give 0.30000000000000004
			"ADD,      -7,   2.5,  -4.5",
			"SUBTRACT, 0.3,  0.1,  0.2",
			"MULTIPLY, -1.5, 4,    -6",
			"MULTIPLY, 0,    123,  0",
			"DIVIDE,   -8,   2,    -4",
			"DIVIDE,   1,    8,    0.125",
	})
	void handlesDecimalsAndNegatives(Operation op, String a, String b, String expected) {
		assertThat(calc(op, a, b).result()).isEqualByComparingTo(expected);
	}

	@Test
	void roundsNonTerminatingDivisionTo34SignificantDigits() {
		assertThat(calc(Operation.DIVIDE, "1", "3").result())
				.isEqualTo(new BigDecimal("0.3333333333333333333333333333333333"));
	}

	@Test
	void normalisesResultsWithoutExponentNotation() {
		assertThat(calc(Operation.DIVIDE, "10", "2").result().toString()).isEqualTo("5");
		assertThat(calc(Operation.ADD, "1.50", "1.50").result().toString()).isEqualTo("3");
		assertThat(calc(Operation.MULTIPLY, "1E+3", "1").result().toString()).isEqualTo("1000");
	}

	@Test
	void echoesNormalisedOperands() {
		CalculationResult r = calc(Operation.ADD, "2.50", "3.0");
		assertThat(r.a().toString()).isEqualTo("2.5");
		assertThat(r.b().toString()).isEqualTo("3");
	}

	@ParameterizedTest
	@CsvSource({ "0", "0.0", "-0.000" })
	void rejectsDivisionByZero(String zero) {
		assertThatThrownBy(() -> calc(Operation.DIVIDE, "5", zero))
				.isInstanceOf(DivisionByZeroException.class);
	}

	@Test
	void rejectsOperandsWithHugeExponents() {
		assertThatThrownBy(() -> calc(Operation.ADD, "1e999999999", "1e-999999999"))
				.isInstanceOf(OperandOutOfRangeException.class)
				.hasMessageContaining("'a'");
	}

	@Test
	void rejectsOperandsWithTooManyDigits() {
		String digits = "9".repeat(CalculatorService.MAX_PRECISION + 1);
		assertThatThrownBy(() -> calc(Operation.MULTIPLY, "2", digits))
				.isInstanceOf(OperandOutOfRangeException.class)
				.hasMessageContaining("'b'");
	}

}
