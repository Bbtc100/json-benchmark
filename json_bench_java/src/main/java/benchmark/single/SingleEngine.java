package benchmark.single;

import benchmark.core.Command;
import benchmark.core.commands.FilterCommand;
import benchmark.core.commands.LengthCommand;
import benchmark.core.commands.MapCommand;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SingleEngine
{
    private final Map<String, Command> commands;

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

    private void register(Command command)
    {
        commands.put(command.name(), command);
    }
}

