package benchmark;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MainTest
{
    @TempDir
    Path tempDir;

    @Test
    void printsHelpForHelpCommand()
    {
        String output = runMainAndCapture("help");

        assertTrue(output.contains("Usage: <engine> <command> <file> [command-args...]"));
        assertTrue(output.contains("single length <file>"));
        assertTrue(output.contains("single filter <file> [expr]"));
        assertTrue(output.contains("single map <file> [expr]"));
    }

    @Test
    void printsHelpForHCommand()
    {
        String output = runMainAndCapture("h");

        assertTrue(output.contains("Commands:"));
        assertTrue(output.contains("help | h"));
    }

    @Test
    void suggestsHelpWhenNoEngineIsProvided()
    {
        String output = runMainAndCapture();

        assertTrue(output.contains("No engine provided."));
        assertTrue(output.contains("Use 'help' or 'h'"));
    }

    @Test
    void suggestsHelpForUnknownEngine()
    {
        String output = runMainAndCapture("unknown", "sample.json");

        assertTrue(output.contains("Unknown engine: unknown"));
        assertTrue(output.contains("Use 'help' or 'h'"));
    }

    @Test
    void suggestsHelpForUnknownCommandInSingleEngine()
    {
        String output = runMainAndCapture("single", "unknown", "sample.json");

        assertTrue(output.contains("Unknown command: unknown"));
        assertTrue(output.contains("Use 'help' or 'h'"));
    }

    @Test
    void printsInputFileNotFound()
    {
        String missingPath = tempDir.resolve("missing.json").toString();

        String output = runMainAndCapture("single", "length", missingPath);

        assertTrue(output.contains("Input file not found:"));
        assertTrue(output.contains(missingPath));
    }

    @Test
    void runsLengthCommandEndToEnd() throws Exception
    {
        Path input = tempDir.resolve("input.json");
        Files.writeString(input, "[1,2,3]", StandardCharsets.UTF_8);

        String output = runMainAndCapture("single", "length", input.toString());

        assertEquals("3", output.trim());
    }

    @Test
    void runsLengthCommandWithFilterExpressionEndToEnd() throws Exception
    {
        Path input = tempDir.resolve("input.json");
        Files.writeString(input, "{\"users\":[1,2,3],\"meta\":true}", StandardCharsets.UTF_8);

        String output = runMainAndCapture("single", "length", input.toString(), ".users");

        assertEquals("3", output.trim());
    }

    private String runMainAndCapture(String... args)
    {
        PrintStream originalOut = System.out;
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        try
        {
            System.setOut(new PrintStream(buffer, true, StandardCharsets.UTF_8));
            Main.main(args);
            return buffer.toString(StandardCharsets.UTF_8);

        } catch (Exception e)
        {
            throw new RuntimeException("Failed to capture output", e);

        } finally
        {
            System.setOut(originalOut);
        }
    }
}

