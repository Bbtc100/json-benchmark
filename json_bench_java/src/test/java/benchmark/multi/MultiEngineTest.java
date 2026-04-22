package benchmark.multi;

import benchmark.single.SingleEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MultiEngineTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final MultiEngine multiEngine = new MultiEngine();
    private final SingleEngine singleEngine = new SingleEngine();

    @Test
    void supportsCoreCommands()
    {
        assertEquals("multi", multiEngine.name());
        assertTrue(multiEngine.commandNames().contains("length"));
        assertTrue(multiEngine.commandNames().contains("filter"));
        assertTrue(multiEngine.commandNames().contains("map"));
    }

    @Test
    void throwsForUnknownCommand() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"a\":1}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> multiEngine.execute("unknown", input, List.of()));

        assertTrue(ex.getMessage().contains("Unknown command"));
    }

    @Test
    void mapMatchesSingleEngineForArrayInput() throws Exception
    {
        JsonNode input = MAPPER.readTree("[{\"n\":1},{\"n\":2},{\"n\":3},{\"n\":4}]");

        JsonNode single = singleEngine.execute("map", input, List.of("+1"));
        JsonNode multi = multiEngine.execute("map", input, List.of("+1"));

        assertEquals(single, multi);
    }

    @Test
    void mapMatchesSingleEngineForObjectInput() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"a\":{\"n\":1},\"b\":{\"n\":2},\"c\":{\"n\":3}}\n");

        JsonNode single = singleEngine.execute("map", input, List.of("+1"));
        JsonNode multi = multiEngine.execute("map", input, List.of("+1"));

        assertEquals(single, multi);
    }

    @Test
    void lengthMatchesSingleEngineForArrayInput() throws Exception
    {
        JsonNode input = MAPPER.readTree("[{\"n\":1},{\"n\":2},{\"n\":3},{\"n\":4}]");

        JsonNode single = singleEngine.execute("length", input, List.of());
        JsonNode multi = multiEngine.execute("length", input, List.of());

        assertEquals(single, multi);
    }

    @Test
    void lengthWithFilterMatchesSingleEngineForObjectInput() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"users\":[1,2,3],\"meta\":true}");

        JsonNode single = singleEngine.execute("length", input, List.of(".users"));
        JsonNode multi = multiEngine.execute("length", input, List.of(".users"));

        assertEquals(single, multi);
    }

    @Test
    void filterMatchesSingleEngineForArrayIterateExpression() throws Exception
    {
        JsonNode input = MAPPER.readTree("[{\"n\":1},{\"n\":2},{\"n\":3},{\"n\":4}]");

        JsonNode single = singleEngine.execute("filter", input, List.of(".[].n"));
        JsonNode multi = multiEngine.execute("filter", input, List.of(".[].n"));

        assertEquals(single, multi);
    }

    @Test
    void supportsConcurrentMapExecution() throws Exception
    {
        JsonNode input = MAPPER.readTree("[{\"n\":1},{\"n\":2},{\"n\":3},{\"n\":4}]");
        JsonNode expected = singleEngine.execute("map", input, List.of("+1"));

        ExecutorService pool = Executors.newFixedThreadPool(4);
        try
        {
            List<Callable<JsonNode>> tasks = new ArrayList<>();
            for (int i = 0; i < 12; i++)
            {
                tasks.add(() -> multiEngine.execute("map", input, List.of("+1")));
            }

            List<Future<JsonNode>> futures = pool.invokeAll(tasks);
            for (Future<JsonNode> future : futures)
            {
                assertEquals(expected, future.get());
            }

        } finally
        {
            pool.shutdownNow();
        }
    }
}

