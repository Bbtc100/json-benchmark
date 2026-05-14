package benchmark.single;

import benchmark.core.Command;
import benchmark.core.StreamingCommand;
import benchmark.core.commands.FilterCommand;
import benchmark.core.commands.LengthCommand;
import benchmark.core.commands.MapCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SingleEngine
{
    private final Map<String, Command> commands;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public SingleEngine()
    {
        this.commands = new HashMap<>();
        register(new LengthCommand());
        register(new FilterCommand());
        register(new MapCommand());
    }

    public String name()
    {
        return "single";
    }

    public Set<String> commandNames()
    {
        return commands.keySet();
    }

    public JsonNode execute(String commandName, JsonNode input, List<String> args)
    {
        Command command = commands.get(commandName);

        if (command == null)
            throw new IllegalArgumentException("Unknown command: " + commandName);

        return command.execute(input, args);
    }

    public void streamExecute(String commandName, Path inputFile, List<String> args, PrintStream out) throws IOException
    {
        Command command = commands.get(commandName);

        if (command == null)
            throw new IllegalArgumentException("Unknown command: " + commandName);

        if (command instanceof StreamingCommand sc)
        {
            sc.streamExecute(inputFile, args, out);
        }
        else
        {
            // fallback, may throw OOM for large files
            JsonNode root = MAPPER.readTree(inputFile.toFile());
            JsonNode result = command.execute(root, args);
            out.println(result.toString());
        }
    }

    private void register(Command command)
    {
        commands.put(command.name(), command);
    }
}

