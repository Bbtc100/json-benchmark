package benchmark.multi;

import benchmark.core.Command;
import benchmark.core.commands.MapCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
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

    public static Callable<byte[]> createFilterTask(List<JsonNode> batch, String expr)
    {
        return () ->
        {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(batch.size() * 256);
            ObjectWriter writer = MAPPER.writer();

            boolean first = true;
            for (JsonNode item : batch)
            {
                JsonNode result = applyStreamingFilterExpression(item, expr);
                if (result == null || result.isNull()) continue;

                if (!first) baos.write(',');
                writer.writeValue(baos, result);
                first = false;
            }

            return baos.toByteArray();
        };

    }

    public static Callable<byte[]> createMapTask(List<JsonNode> batch, String expr)
    {
        return () ->
        {
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream(batch.size() * 256);
            ObjectWriter writer = MAPPER.writer();
            MapCommand mapCommand = new MapCommand();

            boolean first = true;

            for (JsonNode item : batch)
            {
                JsonNode result = mapCommand.execute(item, List.of(expr));
                if (result == null || result.isNull()) continue;

                if (!first)
                    baos.write(',');

                writer.writeValue(baos, result);
                first = false;
            }

            return baos.toByteArray();
        };
    }
}
