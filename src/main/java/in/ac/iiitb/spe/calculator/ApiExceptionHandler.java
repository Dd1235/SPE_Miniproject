package in.ac.iiitb.spe.calculator;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Turns client errors into HTTP 400 responses with an RFC 9457 "problem details" body
 * ({@code application/problem+json}), e.g.
 * {@code {"title":"Division by zero","status":400,"detail":"...","instance":"/api/v1/divide"}}.
 * The optional {@code type} member is omitted, which RFC 9457 defines as {@code about:blank}.
 */
@RestControllerAdvice
public class ApiExceptionHandler {

	/** {@code ?a=2} with no {@code b}, or {@code ?a=&b=3} (present but empty). */
	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ProblemDetail missingParameter(MissingServletRequestParameterException ex) {
		return badRequest("Missing parameter",
				"Query parameter '" + ex.getParameterName() + "' is required and must be a number");
	}

	/** {@code ?a=abc&b=3}: the value could not be converted to a number. */
	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ProblemDetail notANumber(MethodArgumentTypeMismatchException ex) {
		return badRequest("Invalid parameter", "Query parameter '" + ex.getName() + "' must be a number");
	}

	@ExceptionHandler(DivisionByZeroException.class)
	public ProblemDetail divisionByZero(DivisionByZeroException ex) {
		return badRequest("Division by zero", "Parameter 'b' must not be zero for division");
	}

	@ExceptionHandler(OperandOutOfRangeException.class)
	public ProblemDetail outOfRange(OperandOutOfRangeException ex) {
		return badRequest("Operand out of range", "Parameter '" + ex.getParameter()
				+ "' has too many digits or too large an exponent (max " + CalculatorService.MAX_PRECISION
				+ " digits, exponent within ±" + CalculatorService.MAX_SCALE + ")");
	}

	private static ProblemDetail badRequest(String title, String detail) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
		problem.setTitle(title);
		return problem;
	}

}
