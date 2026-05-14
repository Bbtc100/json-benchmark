package benchmark;

import benchmark.multi.MultiEngine;
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
    private static final MultiEngine MULTI_ENGINE = new MultiEngine();

    private static final long STREAMING_THRESHOLD_BYTES = 50L * 1024L  * 1024L;

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

        if (!isKnownEngine(firstArg))
        {
            System.out.println("Unknown engine: " + firstArg);
            System.out.println("Available engines: single, multi");
            System.out.println("Use 'help' or 'h' to see full command documentation.");
            return;
        }

        if (args.length < 2)
        {
            System.out.println("Missing command for engine: " + firstArg);
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

        long startTime = System.currentTimeMillis();

        try
        {
            long fileSize = Files.size(inputPath);

            if (fileSize >  STREAMING_THRESHOLD_BYTES)
            {
                executeStreaming(firstArg, commandName, inputPath, commandArgs);
            }
            else
            {
                JsonNode input = MAPPER.readTree(inputPath.toFile());
                JsonNode output = execute(firstArg, commandName, input, commandArgs);
                System.out.println(output.toString());
            }
        } catch (IOException e)
        {
            System.out.println("Failed to read JSON from file: " + inputPath);
            System.out.println(e.getMessage());

        } catch (RuntimeException e)
        {
            if (e.getMessage() != null && e.getMessage().startsWith("Unknown command:"))
            {
                System.out.println(e.getMessage());
                System.out.println("Available commands for " + firstArg + ": " + String.join(", ", commandNames(firstArg)));
                System.out.println("Use 'help' or 'h' to see full command documentation.");
                return;
            }
            System.out.println("Command failed: " + e.getMessage());
        } finally
        {
            long endTime = System.currentTimeMillis();
            long elapsedTime =  endTime - startTime;
            System.err.println("\nRuntime: " + elapsedTime + " ms");
        }
    }

    private static boolean isKnownEngine(String engineName)
    {
        return "single".equals(engineName) || "multi".equals(engineName);
    }

    private static JsonNode execute(String engineName, String commandName, JsonNode input, List<String> commandArgs)
    {
        return "multi".equals(engineName)
                ? MULTI_ENGINE.execute(commandName, input, commandArgs)
                : SINGLE_ENGINE.execute(commandName, input, commandArgs);
    }

    private static void executeStreaming(String engineName, String commandName, Path inputFile, List<String> commandArgs) throws IOException
    {
        if ("multi".equals(engineName))
        {
            MULTI_ENGINE.streamExecute(commandName, inputFile, commandArgs, System.out);
        }
        else
        {
            SINGLE_ENGINE.streamExecute(commandName, inputFile, commandArgs, System.out);
        }
    }

    private static List<String> commandNames(String engineName)
    {
        return List.copyOf("multi".equals(engineName) ? MULTI_ENGINE.commandNames() : SINGLE_ENGINE.commandNames());
    }

    private static void printHelp()
    {
        System.out.println("Usage: <engine> <command> <file> [command-args...]");
        System.out.println();
        System.out.println("Engines:");
        System.out.println("  single");
        System.out.println("    Runs commands in single-threaded mode.");
        System.out.println("  multi");
        System.out.println("    Runs commands in parallel mode.");
        System.out.println();
        System.out.println("Commands:");
        System.out.println("  <engine> length <file> [expr]");
        System.out.println("    Returns the length of the JSON input.");
        System.out.println("    If expr is provided, it is applied as a filter first.");
        System.out.println("    Arrays/objects -> element count, strings -> character count, others -> 0.");
        System.out.println();
        System.out.println("  <engine> filter <file> [expr]");
        System.out.println("    Applies a simple jq-like path filter expression.");
        System.out.println("    Default expression is '.' (identity).");
        System.out.println("    Path examples: . , .name , .items.[]");
        System.out.println("    Predicate examples:");
        System.out.println("      .users[?id==0]");
        System.out.println("      .users[?age>40]");
        System.out.println("    Supported predicate operators: ==, !=, >, <");
        System.out.println("    Predicate field path is a single field name (no dotted nested path inside predicate).");
        System.out.println();
        System.out.println("  <engine> map <file> [expr]");
        System.out.println("    Applies map_values-like transformation to each object value or array element.");
        System.out.println("    Supports arithmetic expressions like +1, -2, *3, /4, ^0.5.");
        System.out.println("    You may also pass a filter expression (e.g. .name). Default is '.'.");
        System.out.println();
        System.out.println("  help | h");
        System.out.println("    Shows this message.");
    }
}
