/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
