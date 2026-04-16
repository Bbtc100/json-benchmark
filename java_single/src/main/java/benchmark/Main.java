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
        Map<String, Command> commands = new HashMap<>();
        register(commands, new LengthCommand());
        register(commands, new FilterCommand());
        register(commands, new MapCommand());

        // TODO: keys (sorted), has(key), map(f), sort (sort_by(exp)), max/min (max_by(exp))

        if (args.length == 0)
        {
            System.out.println("No command provided.");
            System.out.println("Use 'help' or 'h' to see available commands.");
            return;
        }

        String commandName = args[0];

        if ("help".equals(commandName) || "h".equals(commandName))
        {
            printHelp();
            return;
        }

        if (args.length < 2)
        {
            System.out.println("Missing input file.");
            System.out.println("Use 'help' or 'h' for usage details.");
            return;
        }

        Path inputPath = Path.of(args[1]);
        List<String> commandArgs = args.length > 2
                ? Arrays.asList(args).subList(2, args.length)
                : List.of();

        Command command = commands.get(commandName);

        if (command == null)
        {
            System.out.println("Unknown command: " + commandName);
            System.out.println("Available commands: " + String.join(", ", commands.keySet()));
            System.out.println("Use 'help' or 'h' to see full command documentation.");
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

    private static void printHelp()
    {
        System.out.println("Usage: <command> <file> [command-args...]");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  length <file>");
        System.out.println("    Returns the length of the JSON input.");
        System.out.println("    Arrays/objects -> element count, strings -> character count, others -> 0.");
        System.out.println();
        System.out.println("  filter <file> [expr]");
        System.out.println("    Applies a simple jq-like path filter expression.");
        System.out.println("    Default expression is '.' (identity).");
        System.out.println("    Examples: . , .name , .items.[]");
        System.out.println();
        System.out.println("  map <file> [expr]");
        System.out.println("    Applies map_values-like transformation to each object value or array element.");
        System.out.println("    Supports arithmetic expressions like +1, -2, *3, /4, ^0.5.");
        System.out.println("    You may also pass a filter expression (e.g. .name). Default is '.'.");
        System.out.println();
        System.out.println("  help | h");
        System.out.println("    Shows this message.");
    }
}