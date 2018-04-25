package es.osoco.monkfish;

import org.junit.Test;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Function;

import static org.junit.Assert.*;
import static org.hamcrest.core.Is.is;

public class ResultTests {

    private static final String SUCCESS = "Cool!";
    private static final Integer FIVE = 5;
    private static final Integer NINE = 9;
    private static final Error<String> ERROR = new TestError("Boh");

    @Test
    public void successResult() {
        Result<String> result = Result.success(SUCCESS);
        assertFalse(result.hasErrors());
        assertTrue(result.getValue().isPresent());
        assertEquals(result.getValue().map(String::toUpperCase).orElse(null), SUCCESS.toUpperCase());
        assertEquals(result.get(), SUCCESS);
    }

    @Test
    public void failResult() {
        Result<String> result = Result.fail(ERROR);
        assertTrue(result.hasErrors());
        assertFalse(result.getValue().isPresent());
        assertNull(result.getValue().map(String::toUpperCase).orElse(null));
        try {
            result.get();
            fail("Exception should be thrown");
        } catch (NoSuchElementException ex) {
            assertThat(ex.getMessage(), is("Result not present"));
        }
    }

    @Test
    public void returnAsyncResults() {
        FutureResult<String> future = Result.async(() -> Result.success(SUCCESS));
        assertFalse(future.get().hasErrors());
    }

    @Test
    public void chainResults() {
        FutureResult<Integer> future = Result.async(() -> Result.success(FIVE));
        Result<Integer> doubleTwoTimes = future.get().then(multiplyBy2).thenAsync(multiplyBy2).get();
        assertEquals(doubleTwoTimes.get(), new Integer(20));
    }

    @Test
    public void executeInParallel() {
        List<Result> results = Result.inParallel(() -> Result.success(FIVE), () -> Result.success(NINE));
        assertEquals(results.stream().map(Result::get).reduce((x, y) -> (int)x * (int)y).get(), 45);
    }

    private final Function<Integer, Result<Integer>> multiplyBy2 = ((number) -> Result.success(number * 2));
}
