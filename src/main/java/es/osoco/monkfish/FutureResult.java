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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * Future result, like Java Future, but simplified to only return the result
 */
public class FutureResult<T> {

    private final CompletableFuture<Result<T>> completableFuture;

    FutureResult(Supplier<Result<T>> supplier) {
        this.completableFuture = CompletableFuture.supplyAsync(supplier);
    }

    FutureResult(Result<T> value) {
        this.completableFuture = CompletableFuture.completedFuture(value);
    }

    Result get() {
        Result result;
        try {
            result = completableFuture.get();
        } catch (InterruptedException ie) {
            result = Result.fail(new InterruptedError(ie));
        } catch (ExecutionException ee) {
            result = Result.fail(new ExecutionError(ee));
        }
        return result;
    }
}
