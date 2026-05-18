package benchmark.benchmarks;

import benchmark.multi.MultiEngine;
import benchmark.single.SingleEngine;
import org.openjdk.jmh.annotations.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;

@State(Scope.Benchmark)
public class CommonState
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

    protected Path inputPath;

    public SingleEngine single;
    public MultiEngine multi;

    public static final PrintStream NULL_STREAM =
            new PrintStream(OutputStream.nullOutputStream());

    @Setup(Level.Trial)
    public void setupBase() throws IOException
    {
        inputPath = Path.of("..", "data", fileName);

        single = new SingleEngine();
        multi = new MultiEngine();
    }
}
