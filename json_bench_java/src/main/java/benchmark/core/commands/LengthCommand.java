package benchmark.core.commands;

import benchmark.core.Command;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;

import java.util.List;

public class LengthCommand implements Command
{
	private final FilterCommand filterCommand = new FilterCommand();

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

			input = filterCommand.execute(input, List.of(args.getFirst()));
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
}
