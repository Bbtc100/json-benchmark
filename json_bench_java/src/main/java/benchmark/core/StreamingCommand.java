package benchmark.core;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.List;

public interface StreamingCommand
{
    void streamExecute(Path inputFile, List<String> args, PrintStream out) throws IOException;
}
