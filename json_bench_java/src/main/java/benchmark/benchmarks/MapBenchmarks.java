package benchmark.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class MapBenchmarks
{
    @Benchmark
    public void mapSingle(MapState s, Blackhole bh) throws IOException
    {
        s.single.streamExecute("map", s.inputPath, s.mapArgs, CommonState.NULL_STREAM);
        bh.consume(1);
    }

    @Benchmark
    public void mapMulti(MapState s, Blackhole bh) throws IOException
    {
        s.multi.streamExecute("map", s.inputPath, s.mapArgs, CommonState.NULL_STREAM);
        bh.consume(1);
    }
}
