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
public class LengthBenchmarks
{
    @Benchmark
    public void lengthSingle(LengthState s, Blackhole bh) throws IOException
    {
        s.single.streamExecute(
                "length",
                s.inputPath,
                s.lengthArgs,
                CommonState.NULL_STREAM
        );
        bh.consume(1);
    }

    @Benchmark
    public void lengthMulti(LengthState s, Blackhole bh) throws IOException
    {
        s.multi.streamExecute(
                "length",
                s.inputPath,
                s.lengthArgs,
                CommonState.NULL_STREAM
        );
        bh.consume(1);
    }
}
