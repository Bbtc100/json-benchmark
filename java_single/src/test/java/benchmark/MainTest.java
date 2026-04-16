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

        assertTrue(output.contains("Usage: <command> <file> [command-args...]"));
        assertTrue(output.contains("length <file>"));
        assertTrue(output.contains("filter <file> [expr]"));
        assertTrue(output.contains("map <file> [expr]"));
    }

    @Test
    void printsHelpForHCommand()
    {
        String output = runMainAndCapture("h");

        assertTrue(output.contains("Commands:"));
        assertTrue(output.contains("help | h"));
    }

    @Test
    void suggestsHelpWhenNoCommandIsProvided()
    {
        String output = runMainAndCapture();

        assertTrue(output.contains("No command provided."));
        assertTrue(output.contains("Use 'help' or 'h'"));
    }

    @Test
    void suggestsHelpForUnknownCommand()
    {
        String output = runMainAndCapture("unknown", "sample.json");

        assertTrue(output.contains("Unknown command: unknown"));
        assertTrue(output.contains("Use 'help' or 'h'"));
    }

    @Test
    void printsInputFileNotFound()
    {
        String missingPath = tempDir.resolve("missing.json").toString();

        String output = runMainAndCapture("length", missingPath);

        assertTrue(output.contains("Input file not found:"));
        assertTrue(output.contains(missingPath));
    }

    @Test
    void runsLengthCommandEndToEnd() throws Exception
    {
        Path input = tempDir.resolve("input.json");
        Files.writeString(input, "[1,2,3]", StandardCharsets.UTF_8);

        String output = runMainAndCapture("length", input.toString());

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

