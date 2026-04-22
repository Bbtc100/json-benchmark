package benchmark.core.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MapCommandTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final MapCommand command = new MapCommand();

    @Test
    void mapsObjectValuesRecursivelyWithArithmetic() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\",\"nums\":[1,2,3,4]}");

        JsonNode output = command.execute(input, List.of("+1"));

        assertEquals("{\"name\":\"abc\",\"nums\":[2,3,4,5]}", output.toString());
    }

    @Test
    void supportsOptionalLeadingDotInArithmeticExpression() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"nums\":[1,2,3]}");

        JsonNode output = command.execute(input, List.of(".+1"));

        assertEquals("{\"nums\":[2,3,4]}", output.toString());
    }

    @Test
    void supportsPowerWithNonIntegerExponent() throws Exception
    {
        JsonNode input = MAPPER.readTree("[1,4,9]");

        JsonNode output = command.execute(input, List.of("^0.5"));

        assertEquals("[1,2,3]", output.toString());
    }

    @Test
    void canApplyFilterExpressionToArrayElements() throws Exception
    {
        JsonNode input = MAPPER.readTree("[{\"name\":\"a\"},{\"name\":\"b\"}]");

        JsonNode output = command.execute(input, List.of(".name"));

        assertEquals(MAPPER.readTree("[\"a\",\"b\"]"), output);
    }

    @Test
    void rejectsDivisionByZero() throws Exception
    {
        JsonNode input = MAPPER.readTree("[1,2,3]");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of("/0")));

        assertTrue(ex.getMessage().contains("Division by zero"));
    }

    @Test
    void rejectsPrimitiveRootInput() throws Exception
    {
        JsonNode input = MAPPER.readTree("123");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of("+1")));

        assertTrue(ex.getMessage().contains("expects object or array input"));
    }

    @Test
    void returnsNullForNullInput()
    {
        assertTrue(command.execute(null, List.of("+1")).isNull());
    }

    @Test
    void rejectsInvalidFilterExpressionWithoutDot() throws Exception
    {
        JsonNode input = MAPPER.readTree("[{\"name\":\"a\"}]");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of("name")));

        assertTrue(ex.getMessage().contains("must start with '.'"));
    }
}


