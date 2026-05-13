package benchmark.core.commands;

import benchmark.core.Command;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DecimalNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MapCommand implements Command
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern ARITHMETIC_EXPR = Pattern.compile("^\\s*\\.?\\s*([+\\-*/^])\\s*([+-]?(?:\\d+(?:\\.\\d+)?|\\.\\d+))\\s*$");

    private final FilterCommand filterCommand = new FilterCommand();

    @Override
    public String name() { return "map"; }

    @Override
    public JsonNode execute(JsonNode input, List<String> args)
    {
        if (input == null)
            return com.fasterxml.jackson.databind.node.NullNode.instance;

        String filterExpr = (args == null || args.isEmpty()) ? "." : args.getFirst();

        if (input.isObject())
        {
            ObjectNode mapped = MAPPER.createObjectNode();
            input.fields().forEachRemaining(entry -> mapped.set(
                    entry.getKey(),
                    applyExpression(entry.getValue(), filterExpr)
            ));
            return mapped;
        }

        if (input.isArray())
        {
            ArrayNode mapped = MAPPER.createArrayNode();
            for (JsonNode item : input)
            {
                mapped.add(applyExpression(item, filterExpr));
            }
            return mapped;
        }

        throw new IllegalArgumentException("map_values expects object or array input, got: " + input.getNodeType());
    }

    private JsonNode applyExpression(JsonNode node, String filterExpr)
    {
        ArithmeticOp arithmeticOp = parseArithmetic(filterExpr);

        if (arithmeticOp != null)
        {
            return applyArithmeticRecursively(node, arithmeticOp);
        }

        return filterCommand.execute(node, List.of(filterExpr));
    }

    private ArithmeticOp parseArithmetic(String expr)
    {
        Matcher matcher = ARITHMETIC_EXPR.matcher(expr);

        if (!matcher.matches())
            return null;

        String operator = matcher.group(1);
        BigDecimal operand = new BigDecimal(matcher.group(2));

        if (operator.equals("/") && BigDecimal.ZERO.compareTo(operand) == 0)
            throw new IllegalArgumentException("Division by zero is not allowed in map expression: " + expr);

        return new ArithmeticOp(operator.charAt(0), operand);
    }

    private JsonNode applyArithmeticRecursively(JsonNode node, ArithmeticOp op)
    {
        if (node == null || node.isNull())
            return com.fasterxml.jackson.databind.node.NullNode.instance;

        if (node.isObject())
        {
            ObjectNode result = MAPPER.createObjectNode();
            node.fields().forEachRemaining(entry -> result.set(entry.getKey(), applyArithmeticRecursively(entry.getValue(), op)));
            return result;
        }

        if (node.isArray())
        {
            ArrayNode result = MAPPER.createArrayNode();
            for (JsonNode item : node)
            {
                result.add(applyArithmeticRecursively(item, op));
            }
            return result;
        }

        if (!node.isNumber())
            return node;

        BigDecimal value = node.decimalValue();
        BigDecimal calculated = switch (op.operator)
        {
            case '+' -> value.add(op.operand);
            case '-' -> value.subtract(op.operand);
            case '*' -> value.multiply(op.operand);
            case '/' -> value.divide(op.operand, 16, java.math.RoundingMode.HALF_UP);
            case '^' -> pow(value, op.operand);
            default -> throw new IllegalArgumentException("Unsupported operator: " + op.operator);
        };

        return asJsonNumber(calculated);
    }

    private JsonNode asJsonNumber(BigDecimal value)
    {
        BigDecimal normalized = value.stripTrailingZeros();

        if (normalized.scale() <= 0)
        {
            try
            {
                return LongNode.valueOf(normalized.longValueExact());
            } catch (ArithmeticException ignored) {}
        }

        return DecimalNode.valueOf(normalized);
    }

    private BigDecimal pow(BigDecimal base, BigDecimal exponent)
    {
        BigDecimal normalizedExponent = exponent.stripTrailingZeros();

        if (normalizedExponent.scale() <= 0)
        {
            try
            {
                int intExponent = normalizedExponent.intValueExact();

                if (intExponent >= 0)
                    return base.pow(intExponent);

                if (BigDecimal.ZERO.compareTo(base) == 0)
                    throw new IllegalArgumentException("0 cannot be raised to a negative power.");

                return BigDecimal.ONE.divide(base.pow(-intExponent), 16, RoundingMode.HALF_UP);
            } catch (ArithmeticException ignored) {}
        }

        if (base.compareTo(BigDecimal.ZERO) < 0)
            throw new IllegalArgumentException("Negative base with non-integer exponent is not a real number.");

        double result = Math.pow(base.doubleValue(), exponent.doubleValue());
        if (Double.isNaN(result) || Double.isInfinite(result))
            throw new IllegalArgumentException("Power operation produced a non-finite result.");

        return BigDecimal.valueOf(result);
    }

    private record ArithmeticOp(char operator, BigDecimal operand) {
    }
}
