package in.ac.iiitb.spe.calculator;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Integration tests: start the full Spring application context (controller, service, exception handler,
 * Jackson, Actuator) and send real HTTP requests through Spring MVC via MockMvc.
 */
@SpringBootTest
@AutoConfigureMockMvc
class CalculatorApiIntegrationTests {

	@Autowired
	private MockMvc mvc;

	@Test
	void addReturnsJsonResult() throws Exception {
		mvc.perform(get("/api/v1/add").param("a", "2").param("b", "3"))
			.andExpect(status().isOk())
			.andExpect(content().contentType(MediaType.APPLICATION_JSON))
			.andExpect(content().json("""
					{"operation": "add", "a": 2, "b": 3, "result": 5}
					""", JsonCompareMode.STRICT));
	}

	@ParameterizedTest(name = "GET /api/v1/{0}?a=6&b=3 -> {1}")
	@CsvSource({ "add, 9", "subtract, 3", "multiply, 18", "divide, 2" })
	void everyEndpointComputesItsOperation(String operation, int expected) throws Exception {
		mvc.perform(get("/api/v1/" + operation).param("a", "6").param("b", "3"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.operation").value(operation))
			.andExpect(jsonPath("$.result").value(expected));
	}

	@Test
	void decimalArithmeticIsExact() throws Exception {
		mvc.perform(get("/api/v1/add").param("a", "0.1").param("b", "0.2"))
			.andExpect(status().isOk())
			.andExpect(content().string(containsString("\"result\":0.3}")));
	}

	@Test
	void divisionByZeroReturns400ProblemDetail() throws Exception {
		mvc.perform(get("/api/v1/divide").param("a", "5").param("b", "0"))
			.andExpect(status().isBadRequest())
			.andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
			.andExpect(jsonPath("$.status").value(400))
			.andExpect(jsonPath("$.title").value("Division by zero"))
			.andExpect(jsonPath("$.instance").value("/api/v1/divide"));
	}

	@Test
	void missingParameterReturns400() throws Exception {
		mvc.perform(get("/api/v1/add").param("a", "2"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Missing parameter"))
			.andExpect(jsonPath("$.detail").value(containsString("'b'")));
	}

	@Test
	void emptyParameterReturns400() throws Exception {
		mvc.perform(get("/api/v1/multiply").param("a", "").param("b", "4"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Missing parameter"));
	}

	@Test
	void nonNumericParameterReturns400() throws Exception {
		mvc.perform(get("/api/v1/subtract").param("a", "abc").param("b", "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Invalid parameter"))
			.andExpect(jsonPath("$.detail").value(containsString("'a'")));
	}

	@Test
	void hugeExponentReturns400() throws Exception {
		mvc.perform(get("/api/v1/add").param("a", "1e999999999").param("b", "1"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.title").value("Operand out of range"));
	}

	@Test
	void actuatorHealthIsUp() throws Exception {
		mvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"));
	}

}
