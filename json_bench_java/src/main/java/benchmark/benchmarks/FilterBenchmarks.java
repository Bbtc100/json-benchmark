package benchmark.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class FilterBenchmarks
{
    @Benchmark
    public void filterSingle(FilterState s, Blackhole bh) throws IOException
    {
        s.single.streamExecute(
                "filter",
                s.inputPath,
                List.of(s.filterExpr),
                CommonState.NULL_STREAM
        );
        bh.consume(1);
    }

    @Benchmark
    public void filterMulti(FilterState s, Blackhole bh) throws IOException
    {
        s.multi.streamExecute(
                "filter",
                s.inputPath,
                List.of(s.filterExpr),
                CommonState.NULL_STREAM
        );
        bh.consume(1);
    }
}
