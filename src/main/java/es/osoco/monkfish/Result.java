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

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Result class, has a success result or a fail result with a list of errors
 */
public class Result<T> {

    private final T result;
    private final List<Error> errors;

    /**
     * Create a success Result
     * @param result success result
     * @return T
     */
    static <T> Result success(T result) {
        if (result == null) throw new RuntimeException("Can't create a success result with null");
        return new Result<>(result);
    }

    /**
     * Create a fail result with one error
     * @param error fail error
     * @return Result
     */
    static Result fail(Error error) {
        return new Result<>(Collections.singletonList(error));
    }

    /**
     * Create a fail result with a list of errors
     * @param errors fail errors
     * @return Result
     */
    static Result fail(List<Error> errors) {
        return new Result<>(errors);
    }

    /**
     * Get the result as FutureResult, async
     * @param supplier result
     * @return FutureResult
     */
    static <T> FutureResult<T> async(Supplier<Result<T>> supplier) {
        return new FutureResult<>(supplier);
    }

    /**
     * Get the list of results, executed in parallel
     * @param suppliers results
     * @return List<Result>
     */
    @SafeVarargs
    static List<Result> inParallel(Supplier<Result> ...suppliers) {
        return Arrays.asList(suppliers).parallelStream().map(Supplier::get).collect(Collectors.toList());
    }

    /**
     * Return the successfully result if present
     * @return Optional
     */
    Optional<T> getValue() {
        return result != null ? Optional.of(result) : Optional.empty();
    }

    /**
     * Return errors if present
     * @return Optional
     */
    Optional<List<Error>> getErrors() {
        return errors != null ? Optional.of(errors) : Optional.empty();
    }

    /**
     * Return true if result has errors
     * @return boolean
     */
    boolean hasErrors() {
        return errors != null;
    }

    /**
     * Run the function with the success value, and return the result of that function.
     * If current result is fail return this.
     * @param onSuccessFunction function to execute if result is success
     * @return Result
     */
    Result<?> then(Function<T, Result<?>> onSuccessFunction) {
        if (this.hasErrors()) {
            return this;
        } else {
            return onSuccessFunction.apply(this.result);
        }
    }

    /**
     * Run the function as a future result
     * @param onSuccessFunction function to execute if result is success
     * @return FutureResult
     */
    FutureResult<?> thenAsync(Function<T, Result<?>> onSuccessFunction) {
        if (this.hasErrors()) {
            return new FutureResult<>(this);
        } else {
            return new FutureResult<>(() -> onSuccessFunction.apply(this.result));
        }
    }

    private Result(T result) {
        this.result = result;
        this.errors = null;
    }

    private Result(List<Error> errors) {
        this.result = null;
        this.errors = errors;
    }
}
