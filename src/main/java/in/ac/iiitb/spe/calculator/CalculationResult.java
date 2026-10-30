package in.ac.iiitb.spe.calculator;

import java.math.BigDecimal;

/**
 * Response body for every successful calculation, serialised by Jackson as
 * {@code {"operation":"add","a":2,"b":3,"result":5}}.
 */
public record CalculationResult(String operation, BigDecimal a, BigDecimal b, BigDecimal result) {
}
