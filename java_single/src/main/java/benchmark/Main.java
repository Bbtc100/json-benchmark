package benchmark;

import benchmark.commands.Command;
import benchmark.commands.FilterCommand;
import benchmark.commands.LengthCommand;
import benchmark.commands.MapCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Main {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void main(String[] args) {

        if (args.length < 2)
        {
            System.out.println("Usage: <command> <file> [command-args...]");
            return;
        }

        String commandName = args[0];
        Path inputPath = Path.of(args[1]);
        List<String> commandArgs = args.length > 2
                ? Arrays.asList(args).subList(2, args.length)
                : List.of();

        Map<String, Command> commands = new HashMap<>();
        register(commands, new LengthCommand());
        register(commands, new FilterCommand());
        register(commands, new MapCommand());

        // TODO: keys (sorted), has(key), map(f), sort (sort_by(exp)), max/min (max_by(exp))

        Command command = commands.get(commandName);

        if (command == null)
        {
            System.out.println("Unknown command: " + commandName);
            System.out.println("Available commands: " + String.join(", ", commands.keySet()));
            return;
        }

        if (!Files.exists(inputPath))
        {
            System.out.println("Input file not found: " + inputPath);
            return;
        }

        try
        {
            JsonNode input = MAPPER.readTree(inputPath.toFile());
            JsonNode output = command.execute(input, commandArgs);
            System.out.println(output.toString());

        } catch (IOException e)
        {
            System.out.println("Failed to read JSON from file: " + inputPath);
            System.out.println(e.getMessage());

        } catch (RuntimeException e)
        {
            System.out.println("Command failed: " + e.getMessage());
        }
    }

    private static void register(Map<String, Command> commands, Command command)
    {
        commands.put(command.name(), command);
    }
}