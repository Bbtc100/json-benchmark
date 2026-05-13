package benchmark.core.commands;

import benchmark.core.Command;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.util.ArrayList;
import java.util.List;

public class FilterCommand implements Command
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Override
    public String name() {
        return "filter";
    }

    @Override
    public JsonNode execute(JsonNode input, List<String> args)
    {
        String filterExpr = (args == null || args.isEmpty()) ? "." : args.getFirst();
        return applyFilter(input, filterExpr);
    }

    private JsonNode applyFilter(JsonNode node, String filterExpr)
    {
        List<PathSegment> segments = parseFilter(filterExpr);
        return evaluate(node, segments);
    }

    private List<PathSegment> parseFilter(String expr)
    {
        List<PathSegment> segments = new ArrayList<>();

        if (expr.equals("."))
        {
            segments.add(new PathSegment(PathSegment.Type.IDENTITY, null));
            return segments;
        }

        if (!expr.startsWith("."))
        {
            throw new IllegalArgumentException("Filter must start with '.' (got: " + expr + ")");
        }

        String[] parts = expr.substring(1).split("\\.");

        for (String part : parts)
        {
            if (part.isEmpty())
                continue;

            int predStart = part.indexOf("[?");

            if (predStart >= 0)
            {
                String field = part.substring(0, predStart);
                if (!field.isEmpty())
                {
                    segments.add(new PathSegment(PathSegment.Type.FIELD, field));
                }

                int predEnd =  part.indexOf(']', predStart + 1);
                if (predEnd < 0)
                    throw new IllegalArgumentException("Unterminated expression in: " + expr);

                String pred = part.substring(predStart + 2, predEnd);
                if (pred.isEmpty())
                    throw new IllegalArgumentException("Empty predicate in: " + expr);

                segments.add(new PathSegment(PathSegment.Type.FILTER, pred));

                String tail = part.substring(predEnd + 1);
                if (!tail.isEmpty())
                    throw new IllegalArgumentException("Unexpected characters after predicate in: " + expr);

            } else if (part.equals("[]"))
            {
                segments.add(new PathSegment(PathSegment.Type.ITERATE, null));
            } else
            {
                segments.add(new PathSegment(PathSegment.Type.FIELD, part));
            }
        }

        return segments;
    }

    private JsonNode evaluate(JsonNode node, List<PathSegment> segments)
    {
        if (segments.isEmpty())
            return node;

        PathSegment current = segments.getFirst();
        List<PathSegment> rest = segments.subList(1, segments.size());

        switch (current.type)
        {
            case IDENTITY:
                return rest.isEmpty() ? node : evaluate(node, rest);

            case FIELD:
                if (!node.isObject())
                    return com.fasterxml.jackson.databind.node.NullNode.instance;

                JsonNode field = node.get(current.value);

                if (field == null)
                    return com.fasterxml.jackson.databind.node.NullNode.instance;

                return rest.isEmpty() ? field : evaluate(field, rest);

            case ITERATE:
                if (!node.isArray())
                {
                    throw new IllegalArgumentException("Cannot iterate over non-array type: " + node.getNodeType());
                }
                ArrayNode result = MAPPER.createArrayNode();

                for (JsonNode item : node)
                {
                    JsonNode evaluated = rest.isEmpty() ? item : evaluate(item, rest);
                    result.add(evaluated);
                }
                return result;

            case FILTER:
                if (!node.isArray())
                {
                    throw new IllegalArgumentException("Predicate filter applies to arrays only, found: " + node.getNodeType());
                }

                ArrayNode filtered = MAPPER.createArrayNode();

                for (JsonNode item : node)
                {
                    if (matchesPredicate(item, current.value))
                    {
                        JsonNode evaluated =  rest.isEmpty() ? item : evaluate(item, rest);
                        filtered.add(evaluated);
                    }
                }

                return filtered;

            default:
                throw new IllegalArgumentException("Unknown path segment type: " + current.type);
        }
    }

    private boolean matchesPredicate(JsonNode item, String pred)
    {
        ParsedPredicate predicate = parsePredicate(pred);
        JsonNode target = item.get(predicate.fieldPath());
        if (target == null || target.isNull())
            return false;

        JsonNode literal = parseLiteral(predicate.literal());

        return compare(target, literal, predicate.operator());
    }

    private ParsedPredicate parsePredicate(String raw)
    {
        String trimmed = raw.trim();
        if (trimmed.contains(">=") || trimmed.contains("<="))
            throw new IllegalArgumentException("Predicate must contain one of the following operators: (==, !=, >, <): " + raw);

        int idx;

        idx = trimmed.indexOf("==");
        if (idx >= 0)
            return splitPredicate(trimmed, idx, Operator.EQUALS);

        idx = trimmed.indexOf("!=");
        if (idx >= 0)
            return splitPredicate(trimmed, idx, Operator.NOT_EQUALS);

        idx = trimmed.indexOf(">");
        if (idx >= 0)
            return splitPredicate(trimmed, idx, Operator.GREATER_THAN);

        idx = trimmed.indexOf("<");
        if (idx >= 0)
            return splitPredicate(trimmed, idx, Operator.LESS_THAN);

        throw new IllegalArgumentException("Predicate must contain one of the following operators: (==, !=, >, <): " + raw);
    }

    private ParsedPredicate splitPredicate(String txt, int opIdx, Operator operator)
    {
        int opLen = operator.symbol.length();
        String left =  txt.substring(0, opIdx).trim();
        String right = txt.substring(opIdx + opLen).trim();

        if (left.contains("."))
            throw new IllegalArgumentException("Predicate path must be a single field: " + left);

        if (left.isEmpty() || right.isEmpty())
            throw new IllegalArgumentException("Illegal predicate: " + txt);

        return new ParsedPredicate(left, operator, right);
    }

    private JsonNode parseLiteral(String raw)
    {
        try
        {
            return MAPPER.readTree(raw);
        }
        catch (JsonProcessingException e)
        {
            return new TextNode(raw);
        }
    }

    private boolean compare(JsonNode left, JsonNode right, Operator op)
    {
        return switch (op)
        {
            case EQUALS -> left.equals(right);
            case NOT_EQUALS ->  !left.equals(right);
            case GREATER_THAN -> greaterThan(left, right);
            case LESS_THAN -> lessThan(left, right);
        };
    }

    private boolean greaterThan(JsonNode left, JsonNode right)
    {
        if (left.isNumber() && right.isNumber())
            return left.decimalValue().compareTo(right.decimalValue()) > 0;

        if (left.isTextual() && right.isTextual())
            return left.asText().compareTo(right.asText()) > 0;

        throw new IllegalArgumentException("Cannot compare values with > operator: " + left + " and " + right);
    }

    private boolean lessThan(JsonNode left, JsonNode right)
    {
        if (left.isNumber() && right.isNumber())
            return left.decimalValue().compareTo(right.decimalValue()) < 0;

        if (left.isTextual() && right.isTextual())
            return left.asText().compareTo(right.asText()) < 0;

        throw new IllegalArgumentException("Cannot compare values with < operator: " + left + " and " + right);
    }

    private record ParsedPredicate(String fieldPath, Operator operator, String literal) {}

    private enum Operator
    {
        EQUALS("=="),
        NOT_EQUALS("!="),
        GREATER_THAN(">"),
        LESS_THAN("<");

        private final String symbol;

        Operator(String symbol)
        {
            this.symbol = symbol;
        }
    }

    private static class PathSegment
    {
        enum Type
        {
            IDENTITY,
            FIELD,
            ITERATE,
            FILTER
        }

        Type type;
        String value;

        PathSegment(Type type, String value)
        {
            this.type = type;
            this.value = value;
        }
    }
}

