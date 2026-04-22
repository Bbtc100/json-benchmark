package benchmark.core.commands;

import benchmark.core.Command;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CommandContractTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void allCommandsHaveNonBlankNamesAndReturnResult() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\",\"nums\":[1,2]}");
        List<Command> commands = List.of(
                new LengthCommand(),
                new FilterCommand(),
                new MapCommand()
        );

        for (Command command : commands)
        {
            assertNotNull(command.name());
            assertFalse(command.name().isBlank());

            JsonNode output = switch (command.name())
            {
                case "length" -> command.execute(input, List.of());
                case "filter" -> command.execute(input, List.of("."));
                case "map" -> command.execute(input, List.of("+1"));
                default -> throw new IllegalStateException("Unexpected command: " + command.name());
            };

            assertNotNull(output);
        }
    }
}


