package benchmark.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

public class FilterCommand implements Command
{

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

            if (part.equals("[]"))
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
                ArrayNode result = new ObjectMapper().createArrayNode();

                for (JsonNode item : node)
                {
                    JsonNode evaluated = rest.isEmpty() ? item : evaluate(item, rest);
                    result.add(evaluated);
                }
                return result;

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
            ITERATE
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

