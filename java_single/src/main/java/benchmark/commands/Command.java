package benchmark.commands;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public interface Command
{
    String name();

    JsonNode execute(JsonNode input, List<String> args);
}