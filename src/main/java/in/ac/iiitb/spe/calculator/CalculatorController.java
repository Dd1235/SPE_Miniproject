package in.ac.iiitb.spe.calculator;

import java.math.BigDecimal;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP layer: maps {@code GET /api/v1/{operation}?a=&b=} to {@link CalculatorService}.
 * <p>
 * Spring converts the query parameters to BigDecimal. A missing or non-numeric parameter never reaches
 * these methods; it is turned into an HTTP 400 by {@link ApiExceptionHandler}.
 */
@RestController
@RequestMapping("/api/v1")
public class CalculatorController {

	private final CalculatorService calculator;

	public CalculatorController(CalculatorService calculator) {
		this.calculator = calculator;
	}

	@GetMapping("/add")
	public CalculationResult add(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
		return calculator.calculate(Operation.ADD, a, b);
	}

	@GetMapping("/subtract")
	public CalculationResult subtract(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
		return calculator.calculate(Operation.SUBTRACT, a, b);
	}

	@GetMapping("/multiply")
	public CalculationResult multiply(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
		return calculator.calculate(Operation.MULTIPLY, a, b);
	}

	@GetMapping("/divide")
	public CalculationResult divide(@RequestParam BigDecimal a, @RequestParam BigDecimal b) {
		return calculator.calculate(Operation.DIVIDE, a, b);
	}

}
