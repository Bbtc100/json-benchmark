package benchmark.benchmarks;

import benchmark.multi.MultiEngine;
import benchmark.single.SingleEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
@State(Scope.Benchmark)
public class EngineBenchmarks
{
    @Param({
            "data_10k.json",
            "data_100k.json",
            "data_1M.json",
            "data_10M.json",
            "data_10k_3.json",
            "data_100k_3.json",
            "data_1M_3.json",
            "data_10M_3.json"
    })
    public String fileName;

    @Param({
            "id",
            "age"
    })
    public String filterField;

    @Param({
            "+100",
            "/4",
            "^6"
    })
    public String mapOp;

    @Param({
            //"inmem",
            "stream"
    })
    public String mode;

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode inputRoot;
    private Path inputPath;

    private SingleEngine single;
    private MultiEngine multi;

    private String filterExpr;
    private List<String> mapArgs;
    private List<String> lengthArgs;

    @Setup(Level.Trial)
    public void setup() throws Exception
    {
        Path p = Path.of("..\\data\\" + fileName);

        if ("inmem".equals(mode))
        {
            String json = Files.readString(p);
            inputRoot = mapper.readTree(json);
        }
        else
        {
            inputRoot = null;
            inputPath = p;
        }

        single = new SingleEngine();
        multi = new MultiEngine();

        if (fileName.contains("_3"))
        {
            String predValue = filterField.equals("id") ? "5555" : "3012";
            String operator = filterField.equals("id") ? "==" : ">";
            filterExpr = ".users[?" + filterField + operator + predValue + "].nested_0.nested_1.nested_2";
        }
        else
        {
            String predValue = filterField.equals("id") ? "100" : "4099";
            String operator = filterField.equals("id") ? "<" : "!=";
            filterExpr = ".users[?" + filterField + operator + predValue + "]";
        }

        mapArgs = List.of(mapOp);
        lengthArgs = List.of(".users");
    }

    // MAP benchmarks

    @Benchmark
    public void mapSingle(Blackhole bh) throws IOException
    {
        if ("inmem".equals(mode))
        {
            bh.consume(single.execute("map", inputRoot, mapArgs));
        }
        else
        {
            single.streamExecute("map", inputPath, mapArgs, NULL_STREAM);
            bh.consume(1);
        }
    }

    @Benchmark
    public void mapMulti(Blackhole bh) throws IOException
    {
        if ("inmem".equals(mode))
        {
            bh.consume(multi.execute("map", inputRoot, mapArgs));
        }
        else
        {
            multi.streamExecute("map", inputPath, mapArgs, NULL_STREAM);
            bh.consume(1);
        }

    }

    // LENGTH benchmarks

    @Benchmark
    public void lengthSingle(Blackhole bh) throws IOException
    {
        if ("inmem".equals(mode))
        {
            bh.consume(single.execute("length", inputRoot, lengthArgs));
        }
        else
        {
            single.streamExecute("length", inputPath, lengthArgs, NULL_STREAM);
            bh.consume(1);
        }
    }

    @Benchmark
    public void lengthMulti(Blackhole bh) throws IOException
    {
        if ("inmem".equals(mode))
        {
            bh.consume(multi.execute("length", inputRoot, lengthArgs));
        }
        else
        {
            multi.streamExecute("length", inputPath, lengthArgs, NULL_STREAM);
            bh.consume(1);
        }
    }

    // FILTER benchmarks

    @Benchmark
    public void filterSingle(Blackhole bh) throws IOException
    {
        if ("inmem".equals(mode))
        {
            bh.consume(single.execute("filter", inputRoot, List.of(filterExpr)));
        }
        else
        {
            single.streamExecute("filter", inputPath, List.of(filterExpr), NULL_STREAM);
            bh.consume(1);
        }
    }

    @Benchmark
    public void filterMulti(Blackhole bh) throws IOException
    {
        if ("inmem".equals(mode))
        {
            bh.consume(multi.execute("filter", inputRoot, List.of(filterExpr)));
        }
        else
        {
            multi.streamExecute("filter", inputPath, List.of(filterExpr), NULL_STREAM);
            bh.consume(1);
        }
    }

    private static final PrintStream NULL_STREAM = new PrintStream(java.io.OutputStream.nullOutputStream());
}
