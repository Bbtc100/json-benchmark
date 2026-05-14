package benchmark.core.commands;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.function.Function;

public final class StreamingArrayProcessor
{
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
    private static final ObjectMapper MAPPER = new ObjectMapper();


    public static void processArray(Path inputFile, Function<JsonNode, JsonNode> elementMapper, PrintStream out) throws IOException
    {
        try (JsonParser parser = JSON_FACTORY.createParser(inputFile.toFile()))
        {
            boolean printed = false;
            out.print("[");

            while (parser.nextToken() != null)
            {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "users".equals(parser.currentName()))
                {
                    if (parser.nextToken() != JsonToken.START_ARRAY)
                        break;

                    while (parser.nextToken() != JsonToken.END_ARRAY)
                    {
                        JsonNode element = MAPPER.readTree(parser);
                        JsonNode result = elementMapper.apply(element);

                        if (result != null && !result.isNull())
                        {
                            if (printed)
                                out.print(",");

                            out.print(result);
                            printed = true;
                        }
                    }

                    break;
                }
            }

            out.println("]");
        }
    }
}