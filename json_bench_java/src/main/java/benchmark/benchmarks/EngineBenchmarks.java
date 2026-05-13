package benchmark.benchmarks;

import benchmark.multi.MultiEngine;
import benchmark.single.SingleEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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
            "-10",
            "*15",
            "/4",
            "^6",
            "^0.5"
    })
    public String mapOp;

    private final ObjectMapper mapper = new ObjectMapper();
    private JsonNode inputRoot;

    private SingleEngine single;
    private MultiEngine multi;

    private String filterExpr;
    private List<String> mapArgs;
    private List<String> lengthArgs;

    @Setup(Level.Trial)
    public void setup() throws Exception
    {
        Path p = Path.of("..\\data\\" + fileName);

        String json = Files.readString(p);
        inputRoot = mapper.readTree(json);

        single = new SingleEngine();
        multi = new MultiEngine();

        if (fileName.contains("_3"))
        {
            String predValue = filterField.equals("id") ? "5" : "30";
            String operator = filterField.equals("id") ? "==" : "\">\"";
            filterExpr = ".users[?" + filterField + operator + predValue + "].nested_0.nested_1.nested_2";
        }
        else
        {
            String predValue = filterField.equals("id") ? "10" : "40";
            String operator = filterField.equals("id") ? "\"<\"" : "!=";
            filterExpr = ".users[?" + filterField + operator + predValue + "]";
        }

        mapArgs = List.of(mapOp);
        lengthArgs = List.of(".users");
    }

    @TearDown(Level.Trial)
    public void tearDown() throws Exception
    {}

    // MAP benchmarks

    @Benchmark
    public void mapSingle(Blackhole bh)
    {
        JsonNode out = single.execute("map", inputRoot, mapArgs);
        bh.consume(out);
    }

    @Benchmark
    public void mapMulti(Blackhole bh)
    {
        JsonNode out = multi.execute("map", inputRoot, mapArgs);
        bh.consume(out);
    }

    // LENGTH benchmarks

    @Benchmark
    public void lengthSingle(Blackhole bh)
    {
        JsonNode out = single.execute("length", inputRoot, lengthArgs);
        bh.consume(out);
    }

    @Benchmark
    public void lengthMulti(Blackhole bh)
    {
        JsonNode out = multi.execute("length", inputRoot, lengthArgs);
        bh.consume(out);
    }

    // FILTER benchmarks

    @Benchmark
    public void filterSingle(Blackhole bh)
    {
        JsonNode out = single.execute("filter", inputRoot, List.of(filterExpr));
        bh.consume(out);
    }

    @Benchmark
    public void filterMulti(Blackhole bh)
    {
        JsonNode out = multi.execute("filter", inputRoot, List.of(filterExpr));
        bh.consume(out);
    }
}
