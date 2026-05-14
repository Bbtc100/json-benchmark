package benchmark.core.commands;

import benchmark.core.Command;
import benchmark.core.StreamingCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static benchmark.core.commands.FilterPredicate.*;
import static benchmark.core.commands.StreamingArrayProcessor.processArray;

public class FilterCommand implements Command, StreamingCommand
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

    @Override
    public void streamExecute(Path inputFile, List<String> args, PrintStream out) throws IOException
    {
        String filterExpr = (args == null || args.isEmpty()) ? "." : args.getFirst();

        processArray(inputFile, element -> streamFilterElement(element, filterExpr), out);
    }

    private JsonNode streamFilterElement(JsonNode element, String filterExpr)
    {
        if (filterExpr.startsWith(".users[?"))
        {
            int start =  filterExpr.indexOf("[?");
            int end = filterExpr.indexOf("]", start);

            if (start >= 0 && end > start)
            {
                String predText = filterExpr.substring(start + 2, end);

                try
                {
                    Predicate predicate = parse(predText);
                    if (matches(element, predicate))
                        return element;
                    return null;
                }
                catch (Exception e) {}
            }
        }

        String elementExpr = normalizeElementExpr(filterExpr);
        JsonNode result = applyFilter(element, elementExpr);

        if (result == null || result.isNull())
            return null;

        return result;
    }

    private String normalizeElementExpr(String expr)
    {
        if (expr == null || expr.isBlank())
            return ".";

        if (!expr.startsWith("."))
            throw new IllegalArgumentException("Expression must start with '.': " + expr);

        String body = expr.substring(1);

        if(body.startsWith("users"))
        {
            String tail = body.substring("users".length());
            if (tail.isEmpty())
                return ".";

            return tail.startsWith(".") ? tail : "." + tail;
        }
        return expr;
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

        String body = expr.substring(1);
        int i = 0;
        StringBuilder token = new StringBuilder();
        int bracketDepth = 0;

        while (i < body.length())
        {
            char c = body.charAt(i);

            if (c == '.' && bracketDepth == 0)
            {
                addTokenAsSegment(segments, token.toString(), expr);
                token.setLength(0);
                ++i;
                continue;
            }

            if (c == '[')
            {
                ++bracketDepth;
            }
            else if (c == ']')
            {
                --bracketDepth;
                if (bracketDepth < 0)
                {
                    throw new IllegalArgumentException("Unexpected closing bracket in: " + expr);
                }
            }

            token.append(c);
            ++i;
        }

        if (bracketDepth != 0)
        {
            throw new IllegalArgumentException("Unterminated expression in: " + expr);
        }

        addTokenAsSegment(segments, token.toString(), expr);
        return segments;
    }

    private void addTokenAsSegment(List<PathSegment> segments,String token, String originalExpr)
    {
        if (token == null || token.isBlank())
            return;

        if (token.equals("[]"))
        {
            segments.add(new PathSegment(PathSegment.Type.ITERATE, null));
            return;
        }

        int predStart = token.indexOf("[?");
        if (predStart >= 0)
        {
            String field = token.substring(0, predStart);

            if (!field.isEmpty())
                segments.add(new PathSegment(PathSegment.Type.FIELD, field));

            int predEnd = token.indexOf(']', predStart + 2);
            if (predEnd < 0)
                throw new IllegalArgumentException("Unterminated expression in: " + originalExpr);

            String pred = token.substring(predStart + 2, predEnd);
            if (pred.isEmpty())
                throw new IllegalArgumentException("Empty predicate in: " + originalExpr);

            segments.add(new PathSegment(PathSegment.Type.FILTER, pred));

            String tail = token.substring(predEnd + 1);
            if (!tail.isEmpty())
                throw new IllegalArgumentException("Unexpected characters after predicate in: " + originalExpr);

            return;
        }

        segments.add(new PathSegment(PathSegment.Type.FIELD, token));
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
                    return NullNode.instance;

                JsonNode field = node.get(current.value);

                if (field == null)
                    return NullNode.instance;

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
                Predicate predicate = parse(current.value);

                for (JsonNode item : node)
                {
                    if (matches(item, predicate))
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

