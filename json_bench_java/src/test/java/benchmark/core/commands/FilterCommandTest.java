package benchmark.core.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FilterCommandTest
{
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final FilterCommand command = new FilterCommand();

    @Test
    void identityFilterReturnsInput() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\",\"nums\":[1,2,3]}");

        JsonNode output = command.execute(input, List.of("."));

        assertEquals(input, output);
    }

    @Test
    void defaultFilterIsIdentityWhenArgsMissing() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"x\":1}");

        assertEquals(input, command.execute(input, null));
        assertEquals(input, command.execute(input, List.of()));
    }

    @Test
    void canReadNestedField() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"user\":{\"name\":\"abc\"}}");

        assertEquals("abc", command.execute(input, List.of(".user.name")).asText());
    }

    @Test
    void canIterateArray() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"nums\":[1,2,3]}");

        JsonNode output = command.execute(input, List.of(".nums.[]"));

        assertEquals(MAPPER.readTree("[1,2,3]"), output);
    }

    @Test
    void canFilterByEqualsPredicate() throws Exception
    {
        JsonNode input = MAPPER.readTree("""
                {"users":[
                  {"id":0,"age":75,"nested_0":{"value":15}},
                  {"id":1,"age":35,"nested_0":{"value":99}},
                  {"id":2,"age":35,"nested_0":{"value":15}}
                ]}
                """);

        JsonNode output = command.execute(input, List.of(".users[?id==1]"));

        assertEquals(MAPPER.readTree("""
                [{"id":1,"age":35,"nested_0":{"value":99}}]
                """), output);
    }

    @Test
    void canFilterByNotEqualsPredicate() throws Exception
    {
        JsonNode input = MAPPER.readTree("""
                {"users":[
                  {"id":0,"age":75},
                  {"id":1,"age":35},
                  {"id":2,"age":35}
                ]}
                """);

        JsonNode output = command.execute(input, List.of(".users[?age!=35]"));

        assertEquals(MAPPER.readTree("""
                [{"id":0,"age":75}]
                """), output);
    }

    @Test
    void canFilterByGreaterThanPredicate() throws Exception
    {
        JsonNode input = MAPPER.readTree("""
                {"users":[
                  {"id":0,"age":75},
                  {"id":1,"age":35},
                  {"id":2,"age":40}
                ]}
                """);

        JsonNode output = command.execute(input, List.of(".users[?age>39]"));

        assertEquals(MAPPER.readTree("""
                [{"id":0,"age":75},{"id":2,"age":40}]
                """), output);
    }

    @Test
    void canFilterByLessThanPredicate() throws Exception
    {
        JsonNode input = MAPPER.readTree("""
                {"users":[
                  {"id":0,"age":75},
                  {"id":1,"age":35},
                  {"id":2,"age":40}
                ]}
                """);

        JsonNode output = command.execute(input, List.of(".users[?age<40]"));

        assertEquals(MAPPER.readTree("""
                [{"id":1,"age":35}]
                """), output);
    }

    @Test
    void supportsStringComparisonOperators() throws Exception
    {
        JsonNode input = MAPPER.readTree("""
                {"users":[
                  {"id":0,"name":"anna"},
                  {"id":1,"name":"john"},
                  {"id":2,"name":"zoe"}
                ]}
                """);

        JsonNode output = command.execute(input, List.of(".users[?name>\"john\"]"));

        assertEquals(MAPPER.readTree("""
                [{"id":2,"name":"zoe"}]
                """), output);
    }

    @Test
    void returnsEmptyArrayWhenNoPredicateMatch() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"users\":[{\"id\":1},{\"id\":2}]}");

        JsonNode output = command.execute(input, List.of(".users[?id==999]"));

        assertEquals(MAPPER.readTree("[]"), output);
    }

    @Test
    void failsWhenFilterDoesNotStartWithDot() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of("name")));

        assertTrue(ex.getMessage().contains("must start with '.'"));
    }

    @Test
    void failsWhenTryingToIterateNonArray() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"name\":\"abc\"}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of(".name.[]")));

        assertTrue(ex.getMessage().contains("Cannot iterate over non-array"));
    }

    @Test
    void failsWhenPredicateUsedOnNonArrayNode() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"user\":{\"id\":1}}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of(".user[?id==1]")));

        assertTrue(ex.getMessage().contains("Predicate filter applies to arrays only"));
    }

    @Test
    void failsForTypeMismatchInGreaterThan() throws Exception
    {
        JsonNode input = MAPPER.readTree("""
                {"users":[{"id":1,"name":"a"}]}
                """);

        JsonNode output = command.execute(input, List.of(".users[?name>5]"));

        assertEquals(MAPPER.readTree("[]"), output);
    }

    @Test
    void failsForUnknownPredicateOperator() throws Exception
    {
        JsonNode input = MAPPER.readTree("{\"users\":[{\"id\":1}]}");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> command.execute(input, List.of(".users[?id>=1]")));

        assertTrue(ex.getMessage().contains("Unsupported predicate operator"));
    }

    @Test
    void canFilterAndContinueNestedTail() throws Exception
    {
        JsonNode input = MAPPER.readTree("""
            {"users":[
              {"id":10,"nested_0":{"nested_1":{"value":1}}},
              {"id":20,"nested_0":{"nested_1":{"value":65}}},
              {"id":30,"nested_0":{"nested_1":{"value":2}}}
            ]}
            """);

        JsonNode output = command.execute(input, List.of(".users[?id==20].nested_0.nested_1"));

        assertEquals(MAPPER.readTree("""
            [{"value":65}]
            """), output);
    }
}