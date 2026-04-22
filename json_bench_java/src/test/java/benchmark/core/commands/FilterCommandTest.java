package benchmark.core.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterCommandTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final FilterCommand command = new FilterCommand();

    @Test
    void identityFilterReturnsInput() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\",\"nums\":[1,2,3]}");

        JsonNode output = command.execute(input, List.of("."));

        assertEquals(input, output);
    }

    @Test
    void defaultFilterIsIdentityWhenArgsMissing() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"x\":1}");

        assertEquals(input, command.execute(input, null));
        assertEquals(input, command.execute(input, List.of()));
    }

    @Test
    void canReadNestedField() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"user\":{\"name\":\"abc\"}}");

        assertEquals("abc", command.execute(input, List.of(".user.name")).asText());
    }

    @Test
    void canIterateArray() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"nums\":[1,2,3]}");

        JsonNode output = command.execute(input, List.of(".nums.[]"));

        assertEquals(MAPPER.readTree("[1,2,3]"), output);
    }

    @Test
    void returnsNullForMissingField() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\"}");

        assertTrue(command.execute(input, List.of(".missing")).isNull());
    }

    @Test
    void failsWhenFilterDoesNotStartWithDot() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of("name")));

        assertTrue(ex.getMessage().contains("must start with '.'"));
    }

    @Test
    void failsWhenTryingToIterateNonArray() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of(".name.[]")));

        assertTrue(ex.getMessage().contains("Cannot iterate over non-array"));
    }
}


