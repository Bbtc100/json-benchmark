package benchmark.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class MapCommand implements Command
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final FilterCommand filterCommand = new FilterCommand();

    @Override
    public String name() { return "map"; }

    @Override
    public JsonNode execute(JsonNode input, List<String> args)
    {
        // unchecked solution
        if (input == null)
            return com.fasterxml.jackson.databind.node.NullNode.instance;

        String filterExpr = (args == null || args.isEmpty()) ? "." : args.getFirst();

        if (input.isObject())
        {
            ObjectNode mapped = MAPPER.createObjectNode();
            input.fields().forEachRemaining(entry -> mapped.set(
                    entry.getKey(),
                    filterCommand.execute(entry.getValue(), List.of(filterExpr))
            ));
            return mapped;
        }

        if (input.isArray())
        {
            ArrayNode mapped = MAPPER.createArrayNode();
            for (JsonNode item : input)
            {
                mapped.add(filterCommand.execute(item, List.of(filterExpr)));
            }
            return mapped;
        }

        throw new IllegalArgumentException("map_values expects object or array input, got: " + input.getNodeType());
    }
}
