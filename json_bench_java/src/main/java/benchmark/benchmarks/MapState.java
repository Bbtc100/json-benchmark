package benchmark.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.List;

@State(Scope.Benchmark)
public class MapState extends CommonState
{
    @Param({
            "+100",
            "/4",
            "^6"
    })
    public String mapOp;

    public List<String> mapArgs;

    @Setup(Level.Trial)
    public void setupMap() throws Exception
    {
        super.setupBase();
        mapArgs = List.of(mapOp);
    }
}
