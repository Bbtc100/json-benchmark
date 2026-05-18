package benchmark.benchmarks;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
public class FilterState extends CommonState
{
    @Param({
            "id",
            "age"
    })
    public String filterField;

    public String filterExpr;

    @Setup(Level.Trial)
    public void setupFilter() throws Exception
    {
        super.setupBase();

        if (fileName.contains("_3"))
        {
            String predValue = filterField.equals("id") ? "5555" : "3012";
            String operator = filterField.equals("id") ? "==" : ">";

            filterExpr =
                    ".users[?" + filterField + operator + predValue +
                            "].nested_0.nested_1.nested_2";
        }
        else
        {
            String predValue = filterField.equals("id") ? "100" : "4099";
            String operator = filterField.equals("id") ? "<" : "!=";

            filterExpr =
                    ".users[?" + filterField + operator + predValue + "]";
        }
    }
}
