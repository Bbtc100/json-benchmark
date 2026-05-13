package benchmark.single;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingleEngineTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final SingleEngine engine = new SingleEngine();

    @Test
    void nameIsSingle()
    {
        assertEquals("single", engine.name());
    }

    @Test
    void exposesExpectedCommands()
    {
        assertEquals(Set.of("length", "filter", "map"), engine.commandNames());
    }

    @Test
    void executesLengthCommand() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"users\":[1,2,3]}");

        JsonNode output = engine.execute("length", input, java.util.List.of());

        assertEquals(1, output.asInt());
    }

    @Test
    void executesFilterCommand() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"users\":[{\"id\":0},{\"id\":1}]}");

        JsonNode output = engine.execute("filter", input, java.util.List.of(".users"));

        assertEquals(MAPPER.readTree("[{\"id\":0},{\"id\":1}]"), output);
    }

    @Test
    void executesMapCommand() throws Exception
    {
        JsonNode input = MAPPER.readTree("[1,2,3]");

        JsonNode output = engine.execute("map", input, java.util.List.of("+1"));

        assertEquals("[2,3,4]", output.toString());
    }

    @Test
    void failsForUnknownCommand()
    {
        JsonNode input = MAPPER.nullNode();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> engine.execute("missing", input, java.util.List.of()));

        assertTrue(ex.getMessage().contains("Unknown command"));
    }
}
