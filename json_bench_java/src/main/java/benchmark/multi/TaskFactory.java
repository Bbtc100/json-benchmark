package benchmark.multi;

import benchmark.core.Command;
import benchmark.core.commands.MapCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.List;
import java.util.concurrent.Callable;

import static benchmark.core.commands.FilterCommand.applyStreamingFilterExpression;

public class TaskFactory
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static Callable<Long> createLengthTask(Command command, List<JsonNode> batch, List<String> args)
    {
        return () ->
        {
          ArrayNode chunk = MAPPER.createArrayNode();

          for (JsonNode item : batch)
              chunk.add(item);

          JsonNode result = command.execute(chunk, args);

          if (!result.isNumber())
              throw new IllegalStateException("Expected length command to return a number, but got: " + result);

          return result.asLong();
        };
    }

    public static Callable<JsonNode> createFilterTask(List<JsonNode> batch, String expr)
    {
        return () ->
        {
            ArrayNode filtered = MAPPER.createArrayNode();

            for (JsonNode item : batch)
            {
                JsonNode result = applyStreamingFilterExpression(item, expr);
                if (result != null && !result.isNull())
                    filtered.add(result);
            }

            return filtered;
        };

    }

    public static Callable<JsonNode> createMapTask(List<JsonNode> batch, String expr)
    {
        return () ->
        {
            ArrayNode mapped = MAPPER.createArrayNode();
            MapCommand mapCommand = new MapCommand();

            for (JsonNode item : batch)
            {
                JsonNode result = mapCommand.execute(item, List.of(expr));
                mapped.add(result);
            }

            return mapped;
        };
    }
}
