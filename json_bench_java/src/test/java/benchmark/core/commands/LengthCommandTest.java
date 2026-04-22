package benchmark.core.commands;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LengthCommandTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final LengthCommand command = new LengthCommand();

    @Test
    void returnsZeroForNullInput()
    {
        assertEquals(0, command.execute(null, null).asLong());
    }

    @Test
    void returnsArrayLength() throws Exception
    {
        assertEquals(4, command.execute(MAPPER.readTree("[1,2,3,4]"), null).asLong());
    }

    @Test
    void returnsObjectFieldCount() throws Exception
    {
        assertEquals(3, command.execute(MAPPER.readTree("{\"a\":1,\"b\":2,\"c\":3}"), null).asLong());
    }

    @Test
    void canMeasureFilteredValue() throws Exception
    {
        assertEquals(3, command.execute(MAPPER.readTree("{\"users\":[1,2,3],\"meta\":true}"), List.of(".users")).asLong());
    }

    @Test
    void returnsStringLength() throws Exception
    {
        assertEquals(5, command.execute(MAPPER.readTree("\"hello\""), null).asLong());
    }

    @Test
    void returnsBinaryLength()
    {
        assertEquals(3, command.execute(JsonNodeFactory.instance.binaryNode(new byte[]{1, 2, 3}), null).asLong());
    }

    @Test
    void returnsZeroForNumberAndBoolean() throws Exception
    {
        assertEquals(0, command.execute(MAPPER.readTree("123"), null).asLong());
        assertEquals(0, command.execute(MAPPER.readTree("true"), null).asLong());
    }
}


