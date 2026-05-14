package benchmark.core.commands;

import com.fasterxml.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.util.Objects;

public final class FilterPredicate
{
    public enum Operator
    {
        EQ("=="),
        NE("!="),
        GT(">"),
        LT("<");

        private final String symbol;

        Operator(String symbol)
        {
            this.symbol = symbol;
        }

        public String symbol()
        {
            return symbol;
        }
    }

    public static final class Predicate
    {
        private final String fieldPath;
        private final Operator operator;
        private final String rawValue;
        private final ValueType valueType;

        private Predicate(String fieldPath, Operator operator, String rawValue, ValueType valueType)
        {
            this.fieldPath = fieldPath;
            this.operator = operator;
            this.rawValue = rawValue;
            this.valueType = valueType;
        }

        public String fieldPath()
        {
            return fieldPath;
        }

        public Operator operator()
        {
            return operator;
        }

        public String rawValue()
        {
            return rawValue;
        }

        ValueType valueType()
        {
            return valueType;
        }
    }

    private enum ValueType
    {
        NUMBER,
        BOOLEAN,
        STRING,
        NULL
    }

    public static Predicate parse(String predicateText)
    {
        if (predicateText == null || predicateText.isBlank())
        {
            throw new IllegalArgumentException("Predicate expression must not be blank");
        }

        String expr = predicateText.trim();

        if (expr.contains(">=") || expr.contains("<="))
        {
            throw new IllegalArgumentException("Unsupported predicate operator in: " + predicateText);
        }

        int opPos;
        Operator op = null;

        if ((opPos = expr.indexOf("==")) >= 0)
        {
            op = Operator.EQ;
        }
        else if ((opPos = expr.indexOf("!=")) >= 0)
        {
            op = Operator.NE;
        }
        else if ((opPos = expr.indexOf(">")) >= 0)
        {
            op = Operator.GT;
        }
        else if ((opPos = expr.indexOf("<")) >= 0)
        {
            op = Operator.LT;
        }

        if (op == null)
        {
            throw new IllegalArgumentException("Unknown predicate operator in: " + predicateText);
        }

        String left = expr.substring(0, opPos).trim();
        String right = expr.substring(opPos + op.symbol().length()).trim();

        if (left.isEmpty() || right.isEmpty())
        {
            throw new IllegalArgumentException("Invalid predicate expression: " + predicateText);
        }

        return new Predicate(left, op, normalizeLiteral(right), inferValueType(normalizeLiteral(right)));
    }

    public static boolean matches(JsonNode element, Predicate predicate)
    {
        JsonNode fieldNode = evaluateFieldPath(element, predicate.fieldPath());

        if (fieldNode == null || fieldNode.isMissingNode())
        {
            return predicate.operator() == Operator.EQ && predicate.valueType() == ValueType.NULL;
        }

        return compare(fieldNode, predicate.operator(), predicate.rawValue(), predicate.valueType());
    }

    private static JsonNode evaluateFieldPath(JsonNode root, String fieldPath)
    {
        JsonNode current = root;
        String[] parts = fieldPath.split("\\.");
        for (String part : parts)
        {
            if (part.isBlank())
            {
                return null;
            }
            if (current == null || !current.isObject())
            {
                return null;
            }
            current = current.get(part);
            if (current == null)
            {
                return null;
            }
        }
        return current;
    }

    private static boolean compare(JsonNode leftNode, Operator op, String rightLiteral, ValueType rightType)
    {
        return switch (rightType)
        {
            case NULL -> compareNull(leftNode, op);
            case BOOLEAN -> compareBoolean(leftNode, op, Boolean.parseBoolean(rightLiteral));
            case NUMBER -> compareNumber(leftNode, op, new BigDecimal(rightLiteral));
            case STRING -> compareString(leftNode, op, rightLiteral);
        };
    }

    private static boolean compareNull(JsonNode leftNode, Operator op)
    {
        boolean leftIsNull = leftNode.isNull();
        if (op == Operator.EQ)
        {
            return leftIsNull;
        }
        if (op == Operator.NE)
        {
            return !leftIsNull;
        }
        return false;
    }

    private static boolean compareBoolean(JsonNode leftNode, Operator op, boolean rightValue)
    {
        if (!leftNode.isBoolean())
        {
            return false;
        }

        boolean left = leftNode.booleanValue();
        if (op == Operator.EQ)
        {
            return left == rightValue;
        }
        if (op == Operator.NE)
        {
            return left != rightValue;
        }
        return false;
    }

    private static boolean compareNumber(JsonNode leftNode, Operator op, BigDecimal rightValue)
    {
        if (!leftNode.isNumber())
        {
            return false;
        }

        BigDecimal left = leftNode.decimalValue();
        int cmp = left.compareTo(rightValue);

        if (op == Operator.EQ)
        {
            return cmp == 0;
        }
        if (op == Operator.NE)
        {
            return cmp != 0;
        }
        if (op == Operator.GT)
        {
            return cmp > 0;
        }
        if (op == Operator.LT)
        {
            return cmp < 0;
        }
        return false;
    }

    private static boolean compareString(JsonNode leftNode, Operator op, String rightValue)
    {
        if (!leftNode.isTextual())
        {
            return false;
        }

        String left = leftNode.textValue();
        int cmp = left.compareTo(rightValue);

        if (op == Operator.EQ)
        {
            return Objects.equals(left, rightValue);
        }
        if (op == Operator.NE)
        {
            return !Objects.equals(left, rightValue);
        }
        if (op == Operator.GT)
        {
            return cmp > 0;
        }
        if (op == Operator.LT)
        {
            return cmp < 0;
        }
        return false;
    }

    private static String normalizeLiteral(String raw)
    {
        String trimmed = raw.trim();
        if (trimmed.length() >= 2)
        {
            boolean doubleQuoted = trimmed.startsWith("\"") && trimmed.endsWith("\"");
            boolean singleQuoted = trimmed.startsWith("'") && trimmed.endsWith("'");
            if (doubleQuoted || singleQuoted)
            {
                return trimmed.substring(1, trimmed.length() - 1);
            }
        }
        return trimmed;
    }

    private static ValueType inferValueType(String literal)
    {
        if ("null".equalsIgnoreCase(literal))
        {
            return ValueType.NULL;
        }
        if ("true".equalsIgnoreCase(literal) || "false".equalsIgnoreCase(literal))
        {
            return ValueType.BOOLEAN;
        }
        if (isNumeric(literal))
        {
            return ValueType.NUMBER;
        }
        return ValueType.STRING;
    }

    private static boolean isNumeric(String text)
    {
        try
        {
            new BigDecimal(text);
            return true;
        }
        catch (NumberFormatException ex)
        {
            return false;
        }
    }
}
