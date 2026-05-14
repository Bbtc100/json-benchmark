package benchmark.core.commands;

import benchmark.core.Command;
import benchmark.core.StreamingCommand;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

import static benchmark.core.commands.FilterPredicate.*;

public class LengthCommand implements Command,  StreamingCommand
{
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonFactory JSON_FACTORY = new JsonFactory();

	@Override
	public String name() { return "length"; }

	@Override
	public JsonNode execute(JsonNode input, List<String> args)
    {
		if (args != null && !args.isEmpty())
        {
			if (args.size() > 1)
			{
				throw new IllegalArgumentException("length accepts at most one optional filter expression");
			}

			input = filterAndNavigate(input, args.getFirst());
		}

		if (input == null)
        {
			return LongNode.valueOf(0);
		}

		long length;

		if (input.isArray() || input.isObject())
        {
			length = input.size();

		} else if (input.isTextual())
        {
			length = input.textValue().length();

		} else if (input.isBinary())
        {
			try
            {
				length = input.binaryValue().length;
			} catch (Exception e)
            {
				throw new IllegalStateException("Unable to read binary JSON node", e);
			}
		} else
        {
			length = 0;
		}

		return LongNode.valueOf(length);
	}

	@Override
	public void streamExecute(Path inputFile, List<String> args, PrintStream out) throws IOException
	{
		String expr = (args == null || args.isEmpty()) ? "." : args.get(0);

		if (expr.equals("."))
		{
			try (JsonParser p =  JSON_FACTORY.createParser(inputFile.toFile()))
			{
				while (p.nextToken() != null)
				{
					if (p.currentToken() == JsonToken.START_ARRAY)
					{
						long count = 0;
						while (p.nextToken() != JsonToken.END_ARRAY)
						{
							p.skipChildren();
							++count;
						}
						out.println(count);
						return;
					}
				}
			}
			out.println(0);
			return;
		}

		if (!expr.startsWith("."))
			throw new IllegalArgumentException("Unsupported filter expression for length: " + expr);

		String body = expr.substring(1);
		String fieldName = extractFieldName(body);
		String predText = extractPredicate(body);

		Predicate predicate = predText == null ? null : parse(predText);

		try (JsonParser p =  JSON_FACTORY.createParser(inputFile.toFile()))
		{
			while (p.nextToken() != null)
			{
				if (p.currentToken() == JsonToken.FIELD_NAME && fieldName.equals(p.currentName()))
				{
					if (p.nextToken() != JsonToken.START_ARRAY)
					{
						out.println(0);
						return;
					}

					long count = 0;
					while  (p.nextToken() != JsonToken.END_ARRAY)
					{
						JsonNode item = MAPPER.readTree(p);
						if (predicate == null || matches(item, predicate))
						{
							++count;
						}
					}

					out.println(count);
					return;
				}
			}
		}

		out.println(0);
	}

	private JsonNode filterAndNavigate(JsonNode input, String expr)
	{
		if (!expr.startsWith("."))
			throw new IllegalArgumentException("Expression must start with '.': " + expr);

		String body = expr.substring(1);
		String fieldName = extractFieldName(body);
		String predText = extractPredicate(body);

		JsonNode current = input;
		if (!fieldName.isEmpty() && !fieldName.equals("."))
			current = current.get(fieldName);

		if (current == null || !current.isArray())
			return null;

		if (predText == null)
			return current;

		Predicate predicate = parse(predText);
		ArrayNode filtered = MAPPER.createArrayNode();

		for (JsonNode item : current)
		{
			if (matches(item, predicate))
				filtered.add(item);
		}

		return filtered;
	}

	private String extractFieldName(String body)
	{
		int bracketPos = body.indexOf('[');
		if (bracketPos >= 0)
			return body.substring(0, bracketPos);

		return body;
	}

	private String extractPredicate(String body)
	{
		int start = body.indexOf("[?");
		if (start < 0)
			return null;

		int end = body.indexOf(']', start);
		if (end < 0)
			throw new IllegalArgumentException("Unterminated predicate in: " + body);

		return body.substring(start + 2, end);
	}
}
