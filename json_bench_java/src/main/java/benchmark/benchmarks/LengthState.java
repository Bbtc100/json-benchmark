package benchmark.benchmarks;

import org.openjdk.jmh.annotations.*;

import java.util.List;

@State(Scope.Benchmark)
public class LengthState extends CommonState
{
    public List<String> lengthArgs;

    @Setup(Level.Trial)
    public void setupLength() throws Exception
    {
        super.setupBase();
        lengthArgs = List.of(".users");
    }
}
