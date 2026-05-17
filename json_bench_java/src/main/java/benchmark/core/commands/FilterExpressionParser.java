package benchmark.core.commands;

public class FilterExpressionParser
{
    public record Parts (String prefix, String predicateText, String tail) {}

    public static Parts split(String filterExpr)
    {
        if (filterExpr == null || filterExpr.isBlank() || ".".equals(filterExpr))
            return new Parts("", null, null);

        if (!filterExpr.startsWith("."))
            throw new IllegalArgumentException("Expression must start with '.'");

        String body = filterExpr.substring(1);
        if (body.startsWith("users"))
            body = body.substring("users".length());

        int predStart = body.indexOf("[?");
        if (predStart < 0)
            return new Parts(body, null, null);

        int predEnd = body.indexOf("]", predStart + 2);
        if (predEnd < 0)
            throw new IllegalArgumentException("Unterminated predicate in filter expression: " + filterExpr);

        String prefix = body.substring(0, predStart);
        String predicateText = body.substring(predStart + 2, predEnd).trim();
        String tail = body.substring(predEnd + 1);

        if (tail != null && tail.isBlank())
            tail = null;

        return new Parts(prefix, predicateText, tail);
    }
}
