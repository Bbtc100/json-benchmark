package benchmark.multi;

import benchmark.core.Command;
import benchmark.core.StreamingCommand;
import benchmark.core.commands.FilterCommand;
import benchmark.core.commands.FilterPredicate;
import benchmark.core.commands.LengthCommand;
import benchmark.core.commands.MapCommand;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
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

import static benchmark.core.commands.FilterPredicate.*;

public class MultiEngine
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonFactory JSON_FACTORY = new JsonFactory();
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

    public void streamExecute(String commandName, Path inputFile, List<String> args, PrintStream out) throws IOException
    {
        List<String> safeArgs = args == null ? List.of() : List.copyOf(args);
        Command command = commands.get(commandName);

        if (command == null)
            throw new IllegalArgumentException("Unknown command: " + commandName);

        switch (commandName)
        {
            case "length" -> streamLength(command, inputFile, safeArgs, out);
            case "filter" -> streamFilter(inputFile, safeArgs, out);
            case "map" -> streamMap(inputFile, safeArgs, out);
            default ->
            {
                if (command instanceof StreamingCommand sc)
                {
                    sc.streamExecute(inputFile, safeArgs, out);
                }
                else
                {
                    // fallback, may throw OOM for large files
                    JsonNode root = MAPPER.readTree(inputFile.toFile());
                    JsonNode result = command.execute(root, safeArgs);
                    out.println(result.toString());
                }
            }
        }
    }

    private void streamLength(Command command, Path inputFile, List<String> args, PrintStream out) throws IOException
    {
           try (JsonParser parser = JSON_FACTORY.createParser(inputFile.toFile()))
           {
               while (parser.nextToken() != null)
               {
                   if (parser.currentToken() == JsonToken.FIELD_NAME && "users".equals(parser.currentName()))
                   {
                       if (parser.nextToken() != JsonToken.START_ARRAY)
                       {
                           out.println(0);
                           return;
                       }

                       List<Callable<Long>> tasks = new ArrayList<>();
                       List<JsonNode> batch = new ArrayList<>(chunkSizeHint());

                       while (parser.nextToken() != JsonToken.END_ARRAY)
                       {
                           batch.add(MAPPER.readTree(parser));
                           if (batch.size() >= chunkSizeHint())
                           {
                               tasks.add(lengthTask(command, batch, args));
                               batch = new  ArrayList<>(chunkSizeHint());
                           }
                       }

                       if (!batch.isEmpty())
                           tasks.add(lengthTask(command, batch, args));

                       long total = 0;
                       for (Future<Long> future : invokeLongTasks(tasks))
                       {
                           try
                           {
                               total += future.get();
                           }
                           catch (InterruptedException e)
                           {
                               Thread.currentThread().interrupt();
                               throw new RuntimeException("Parallel execution was interrupted", e);
                           }
                           catch (ExecutionException e)
                           {
                               Throwable cause = e.getCause();
                               if (cause instanceof RuntimeException runtimeException)
                                   throw runtimeException;

                               throw new RuntimeException("Parallel execution failed", cause);
                           }
                       }

                       out.println(total);
                       return;
                   }
               }
           }
           catch (InterruptedException e)
           {
               Thread.currentThread().interrupt();
               throw new RuntimeException("Parallel execution was interrupted", e);
           }

           out.println(0);
    }

    private void streamFilter(Path inputFile, List<String> args, PrintStream out) throws IOException
    {
        String expr = args.isEmpty() ? "." : args.getFirst();

        try(JsonParser parser = JSON_FACTORY.createParser(inputFile.toFile()))
        {
            while (parser.nextToken() != null)
            {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "users".equals(parser.currentName()))
                {
                    if (parser.nextToken() != JsonToken.START_ARRAY)
                    {
                        out.println("[]");
                        return;
                    }

                    List<Callable<JsonNode>> tasks = new ArrayList<>();
                    List<JsonNode> batch = new ArrayList<>(chunkSizeHint());

                    while (parser.nextToken() != JsonToken.END_ARRAY)
                    {
                        batch.add(MAPPER.readTree(parser));
                        if (batch.size() >= chunkSizeHint())
                        {
                            tasks.add(filterTask(batch, expr));
                            batch = new  ArrayList<>(chunkSizeHint());
                        }
                    }

                    if (!batch.isEmpty())
                        tasks.add(filterTask(batch, expr));

                    List<JsonNode> parts = invokeTasks(tasks);
                    out.println(mergeArrayParts(parts).toString());
                    return;
                }
            }
        }

        out.println("[]");
    }

    private void streamMap(Path inputFile, List<String> args, PrintStream out) throws IOException
    {
        String expr = args.isEmpty() ? "." : args.getFirst();

        try(JsonParser parser = JSON_FACTORY.createParser(inputFile.toFile()))
        {
            while (parser.nextToken() != null)
            {
                if (parser.currentToken() == JsonToken.FIELD_NAME && "users".equals(parser.currentName()))
                {
                    if (parser.nextToken() != JsonToken.START_ARRAY)
                    {
                        out.println("[]");
                        return;
                    }

                    List<Callable<JsonNode>> tasks = new ArrayList<>();
                    List<JsonNode> batch = new ArrayList<>(chunkSizeHint());

                    while (parser.nextToken() != JsonToken.END_ARRAY)
                    {
                        batch.add(MAPPER.readTree(parser));
                        if (batch.size() >= chunkSizeHint())
                        {
                            tasks.add(mapTask(batch, expr));
                            batch = new  ArrayList<>(chunkSizeHint());
                        }
                    }

                    if (!batch.isEmpty())
                        tasks.add(mapTask(batch, expr));

                    List<JsonNode> parts = invokeTasks(tasks);
                    out.println(mergeArrayParts(parts).toString());
                    return;
                }
            }
        }

        out.println("[]");
    }

    private Callable<Long> lengthTask(Command command, List<JsonNode> batch, List<String> args)
    {
        ArrayNode chunk = MAPPER.createArrayNode();

        for (JsonNode item : batch)
        {
            chunk.add(item);
        }
        return () ->
        {
            JsonNode result = command.execute(chunk, args);
            if (!result.isNumber())
                throw new IllegalStateException("Parallel length chunk returned non-numeric output");

            return result.longValue();
        };
    }

    private Callable<JsonNode> filterTask(List<JsonNode> batch, String expr)
    {
        return () ->
        {
            ArrayNode filtered = MAPPER.createArrayNode();
            FilterExpressionParts parts = splitFilterExpression(expr);

             for (JsonNode item : batch)
             {
                 JsonNode result = applyStreamingFilterExpression(item, expr);
                 if (result != null && !result.isNull())
                 {
                     filtered.add(result);
                 }
             }
             return filtered;
        };
    }

    private Callable<JsonNode> mapTask(List<JsonNode> batch, String expr)
    {
        return () ->
        {
            ArrayNode mapped = MAPPER.createArrayNode();
            MapCommand mapCommand = new  MapCommand();

            String elementExpr = normalizeElementExpr(expr);
            for (JsonNode item : batch)
            {
                JsonNode result = mapCommand.execute(item, List.of(elementExpr));
                mapped.add(result);
            }
            return mapped;
        };
    }

    private String normalizeElementExpr(String expr)
    {
        if (expr == null || expr.isBlank())
            return ".";

        if (!expr.startsWith("."))
            throw new IllegalArgumentException("Expression must start with '.': " + expr);

        String body = expr.substring(1);

        if(body.startsWith("users"))
        {
            String tail = body.substring("users".length());
            if (tail.isEmpty())
                return ".";

            return tail.startsWith(".") ? tail : "." + tail;
        }
        return expr;
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

        return LongNode.valueOf(total);
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

    private JsonNode applyStreamingFilterExpression(JsonNode node, String expr)
    {
        if (expr == null || expr.isBlank() || ".".equals(expr))
            return node;

        FilterExpressionParts parts = splitFilterExpression(expr);

        if (parts.predicateText() == null)
        {
            FilterCommand filterCommand = new FilterCommand();
            JsonNode result = filterCommand.execute(node, List.of(expr));
            return (result == null || result.isNull()) ? null : result;
        }

        Predicate predicate = parse(parts.predicateText());
        if (!matches(node, predicate))
            return null;

        if (parts.tail() == null || parts.tail().isBlank())
            return node;

        String tailExpr = parts.tail().startsWith(".") ? parts.tail() : "." + parts.tail();
        FilterCommand filterCommand = new FilterCommand();
        JsonNode result = filterCommand.execute(node, List.of(tailExpr));

        return (result == null || result.isNull()) ? null : result;
    }

    private FilterExpressionParts splitFilterExpression(String filterExpr)
    {
        if (filterExpr == null || filterExpr.isBlank() || ".".equals(filterExpr))
            return new FilterExpressionParts(".", null, null);

        if (!filterExpr.startsWith("."))
            throw new IllegalArgumentException("Expression must start with '.'");

        String body = filterExpr.substring(1);
        if (body.startsWith("users"))
            body = body.substring("users".length());

        int predStart = body.indexOf("[?");
        if (predStart < 0)
            return new FilterExpressionParts(body, null, null);

        int predEnd = body.indexOf("]", predStart + 2);
        if (predEnd < 0)
            throw new IllegalArgumentException("Unterminated predicate in filter expression: " + filterExpr);

        String prefix = body.substring(0, predStart);
        String predicateText = body.substring(predStart + 2, predEnd).trim();
        String tail = body.substring(predEnd + 1);

        if (tail != null && tail.isBlank())
            tail = null;

        return new FilterExpressionParts(prefix, predicateText, tail);
    }

    private record FilterExpressionParts(String prefix, String predicateText, String tail) {}

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

    private List<Future<Long>> invokeLongTasks(List<Callable<Long>> tasks) throws InterruptedException
    {
            try
            {
                return executor.invokeAll(tasks);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Parallel execution was interrupted", e);
            }
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

    private int chunkSizeHint()
    {
        return Math.max(1, 1024 / workers);
    }

    private void register(Command command)
    {
        commands.put(command.name(), command);
    }
}
