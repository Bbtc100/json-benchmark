package benchmark.multi;

import benchmark.core.Command;
import benchmark.core.commands.FilterCommand;
import benchmark.core.commands.LengthCommand;
import benchmark.core.commands.MapCommand;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class MultiEngine
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final AtomicInteger WORKER_ID = new AtomicInteger(1);

    private final Map<String, Command> commands;
    private final int workers;
    private final ExecutorService executor;

    public MultiEngine()
    {
        Map<String, Command> commandRegistry = new HashMap<>();
        this.workers = Math.max(1, Runtime.getRuntime().availableProcessors());
        this.executor = Executors.newFixedThreadPool(workers, daemonThreadFactory());

        this.commands = commandRegistry;
        register(new LengthCommand());
        register(new FilterCommand());
        register(new MapCommand());
    }

    public String name()
    {
        return "multi";
    }

    public Set<String> commandNames()
    {
        return Set.copyOf(commands.keySet());
    }

    public JsonNode execute(String commandName, JsonNode input, List<String> args)
    {
        List<String> safeArgs = args == null ? List.of() : List.copyOf(args);
        Command command = commands.get(commandName);

        if (command == null)
            throw new IllegalArgumentException("Unknown command: " + commandName);

        return switch (commandName)
        {
            case "map" -> executeMapInParallel(command, input, safeArgs);
            case "filter" -> executeFilterInParallel(command, input, safeArgs);
            case "length" -> executeLengthInParallel(command, input, safeArgs);
            default -> command.execute(input, safeArgs);
        };
    }

    private JsonNode executeMapInParallel(Command command, JsonNode input, List<String> args)
    {
        if (input == null)
            return command.execute(null, args);

        if (input.isArray())
            return mergeArrayParts(executeOnArrayChunks(command, input, args));

        if (input.isObject())
            return mergeObjectParts(executeOnObjectChunks(command, input, args));

        return command.execute(input, args);
    }

    private JsonNode executeFilterInParallel(Command command, JsonNode input, List<String> args)
    {
        if (input == null)
            return command.execute(null, args);

        String expr = args.isEmpty() ? "." : args.getFirst();

        if (input.isArray() && (".".equals(expr) || expr.startsWith(".[]")))
            return mergeArrayParts(executeOnArrayChunks(command, input, args));

        if (input.isObject() && ".".equals(expr))
            return mergeObjectParts(executeOnObjectChunks(command, input, args));

        return command.execute(input, args);
    }

    private JsonNode executeLengthInParallel(Command command, JsonNode input, List<String> args)
    {
        if (input == null)
            return command.execute(null, args);

        if (!input.isArray() && !input.isObject())
            return command.execute(input, args);

        List<JsonNode> partials = input.isArray()
                ? executeOnArrayChunks(command, input, args)
                : executeOnObjectChunks(command, input, args);

        long total = 0;
        for (JsonNode partial : partials)
        {
            if (!partial.isNumber())
                throw new IllegalStateException("Parallel length chunk returned non-numeric output");
            total += partial.longValue();
        }

        return com.fasterxml.jackson.databind.node.LongNode.valueOf(total);
    }

    private List<JsonNode> executeOnArrayChunks(Command command, JsonNode input, List<String> args)
    {
        int size = input.size();
        if (size < 2)
            return List.of(command.execute(input, args));

        List<Callable<JsonNode>> tasks = new ArrayList<>();
        int chunkSize = chunkSize(size);

        for (int start = 0; start < size; start += chunkSize)
        {
            int end = Math.min(start + chunkSize, size);
            ArrayNode chunk = MAPPER.createArrayNode();
            for (int i = start; i < end; i++)
            {
                chunk.add(input.get(i));
            }

            tasks.add(() -> command.execute(chunk, args));
        }

        return invokeTasks(tasks);
    }

    private List<JsonNode> executeOnObjectChunks(Command command, JsonNode input, List<String> args)
    {
        List<Map.Entry<String, JsonNode>> entries = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = input.fields();
        while (fields.hasNext())
        {
            Map.Entry<String, JsonNode> field = fields.next();
            entries.add(new AbstractMap.SimpleEntry<>(field.getKey(), field.getValue()));
        }

        int size = entries.size();
        if (size < 2)
            return List.of(command.execute(input, args));

        List<Callable<JsonNode>> tasks = new ArrayList<>();
        int chunkSize = chunkSize(size);

        for (int start = 0; start < size; start += chunkSize)
        {
            int end = Math.min(start + chunkSize, size);
            ObjectNode chunk = MAPPER.createObjectNode();
            for (int i = start; i < end; i++)
            {
                Map.Entry<String, JsonNode> entry = entries.get(i);
                chunk.set(entry.getKey(), entry.getValue());
            }

            tasks.add(() -> command.execute(chunk, args));
        }

        return invokeTasks(tasks);
    }

    private List<JsonNode> invokeTasks(List<Callable<JsonNode>> tasks)
    {
        List<Future<JsonNode>> futures;
        try
        {
            futures = executor.invokeAll(tasks);
        } catch (InterruptedException e)
        {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Parallel execution was interrupted", e);
        }

        List<JsonNode> results = new ArrayList<>(futures.size());
        for (Future<JsonNode> future : futures)
        {
            try
            {
                results.add(future.get());
            } catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel execution was interrupted", e);
            } catch (ExecutionException e)
            {
                Throwable cause = e.getCause();
                if (cause instanceof RuntimeException runtimeException)
                    throw runtimeException;

                throw new RuntimeException("Parallel execution failed", cause);
            }
        }

        return results;
    }

    private JsonNode mergeArrayParts(List<JsonNode> parts)
    {
        ArrayNode merged = MAPPER.createArrayNode();

        for (JsonNode partial : parts)
        {
            if (!partial.isArray())
                throw new IllegalStateException("Parallel chunk returned non-array output");

            for (JsonNode item : partial)
            {
                merged.add(item);
            }
        }

        return merged;
    }

    private JsonNode mergeObjectParts(List<JsonNode> parts)
    {
        ObjectNode merged = MAPPER.createObjectNode();

        for (JsonNode partial : parts)
        {
            if (!partial.isObject())
                throw new IllegalStateException("Parallel chunk returned non-object output");

            partial.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));
        }

        return merged;
    }

    private ThreadFactory daemonThreadFactory()
    {
        return runnable ->
        {
            Thread thread = new Thread(runnable, "multi-engine-worker-" + WORKER_ID.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        };
    }

    private int chunkSize(int size)
    {
        return Math.max(1, (size + workers - 1) / workers);
    }

    private void register(Command command)
    {
        commands.put(command.name(), command);
    }
}
