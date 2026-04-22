package benchmark;

import benchmark.single.SingleEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

public class Main {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final SingleEngine SINGLE_ENGINE = new SingleEngine();

    public static void main(String[] args) {
        if (args.length == 0)
        {
            System.out.println("No engine provided.");
            System.out.println("Use 'help' or 'h' to see available commands.");
            return;
        }

        String firstArg = args[0];

        if ("help".equals(firstArg) || "h".equals(firstArg))
        {
            printHelp();
            return;
        }

        if (!"single".equals(firstArg))
        {
            System.out.println("Unknown engine: " + firstArg);
            System.out.println("Available engines: single");
            System.out.println("Use 'help' or 'h' to see full command documentation.");
            return;
        }

        if (args.length < 2)
        {
            System.out.println("Missing command for engine: single");
            System.out.println("Use 'help' or 'h' for usage details.");
            return;
        }

        String commandName = args[1];

        if (args.length < 3)
        {
            System.out.println("Missing input file.");
            System.out.println("Use 'help' or 'h' for usage details.");
            return;
        }

        Path inputPath = Path.of(args[2]);
        List<String> commandArgs = args.length > 3
                ? Arrays.asList(args).subList(3, args.length)
                : List.of();

        if (!Files.exists(inputPath))
        {
            System.out.println("Input file not found: " + inputPath);
            return;
        }

        try
        {
            JsonNode input = MAPPER.readTree(inputPath.toFile());
            JsonNode output = SINGLE_ENGINE.execute(commandName, input, commandArgs);
            System.out.println(output.toString());

        } catch (IOException e)
        {
            System.out.println("Failed to read JSON from file: " + inputPath);
            System.out.println(e.getMessage());

        } catch (RuntimeException e)
        {
            if (e.getMessage() != null && e.getMessage().startsWith("Unknown command:"))
            {
                System.out.println(e.getMessage());
                System.out.println("Available commands for single: " + String.join(", ", SINGLE_ENGINE.commandNames()));
                System.out.println("Use 'help' or 'h' to see full command documentation.");
                return;
            }
            System.out.println("Command failed: " + e.getMessage());
        }
    }

    private static void printHelp()
    {
        System.out.println("Usage: <engine> <command> <file> [command-args...]");
        System.out.println();
        System.out.println("Engines:");
        System.out.println("  single");
        System.out.println("    Runs commands in single-threaded mode.");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  single length <file>");
        System.out.println("    Returns the length of the JSON input.");
        System.out.println("    Arrays/objects -> element count, strings -> character count, others -> 0.");
        System.out.println();
        System.out.println("  single filter <file> [expr]");
        System.out.println("    Applies a simple jq-like path filter expression.");
        System.out.println("    Default expression is '.' (identity).");
        System.out.println("    Examples: . , .name , .items.[]");
        System.out.println();
        System.out.println("  single map <file> [expr]");
        System.out.println("    Applies map_values-like transformation to each object value or array element.");
        System.out.println("    Supports arithmetic expressions like +1, -2, *3, /4, ^0.5.");
        System.out.println("    You may also pass a filter expression (e.g. .name). Default is '.'.");
        System.out.println();
        System.out.println("  help | h");
        System.out.println("    Shows this message.");
    }
}