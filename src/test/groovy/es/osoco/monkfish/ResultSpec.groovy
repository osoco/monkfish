/*
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
package es.osoco.monkfish

import spock.lang.Specification
import spock.lang.Unroll

import java.util.concurrent.ExecutionException
import java.util.concurrent.atomic.AtomicInteger
import java.util.function.Function
import java.util.function.Supplier

/**
 * JFL 14/4/18
 */
class ResultSpec extends Specification {

    private static final String GOOD_RESULT = 'aResult'
    private static final String EXCEPTION_MESSAGE = 'an exception'

    void 'test result creation'() {
        expect:
        Result.success(5)
        Result.success("hello")
        Result.fail(Stub(Error))
        Result.fail([Stub(Error), Stub(Error)])
    }

    void 'can not create a result with null'() {
        when:
        Result.success(null)

        then:
        thrown(RuntimeException)
    }

    void 'success result'() {
        when:
        Result<String> result = Result.success(GOOD_RESULT)

        then:
        !result.hasErrors()
        result.value.isPresent()
        !result.errors.isPresent()
        result.get() == GOOD_RESULT
    }

    void 'error result'() {
        given:
        Error error = Stub(Error)

        when:
        Result<String> result = Result.fail(error)

        then:
        result.hasErrors()
        !result.value.isPresent()
        result.errors.get() == [error]
    }

    void 'async result'() {
        given:
        AtomicInteger count = new AtomicInteger(0)

        when:
        FutureResult futureResult = Result.async {
            sleep(10)
            assert count.getAndIncrement() == 0
            Result.success(GOOD_RESULT)
        }
        
        then:
        count.get() == 0

        when:
        Result result = futureResult.get()

        then:
        result.value.get() == GOOD_RESULT
        count.get() == 1
    }

    void 'error getting async result'() {
        given:
        FutureResult futureResult = Result.async {
            throw new RuntimeException(EXCEPTION_MESSAGE)
        }

        when:
        Result result =futureResult.get()
        List<Error<Exception>> errors = result.errors.get()
        Error<ExecutionException> firstError = errors.first()

        then:
        errors.size() == 1
        !firstError.isFutureError()
        firstError.isExecutionError()
        firstError.code.cause instanceof RuntimeException
        firstError.code.cause.message == EXCEPTION_MESSAGE
    }

    @Unroll
    void 'execute in parallel'() {
        when:
        List<Result> results = Result.inParallel(
                { -> firstResult } as Supplier<Result>,
                { -> secondResult } as Supplier<Result>)

        then:
        results*.value*.isPresent() == expectedResult

        where:
        firstResult                 | secondResult                | expectedResult
        Result.success(GOOD_RESULT) | Result.success(GOOD_RESULT) | [true, true]
        Result.fail(Stub(Error))    | Result.success(GOOD_RESULT) | [false, true]
        Result.success(GOOD_RESULT) | Result.fail(Stub(Error))    | [true, false]
        Result.fail(Stub(Error))    | Result.fail(Stub(Error))    | [false, false]
    }

    void 'get success result and later double it'() {
        given:
        Result result = Result.success(2)

        when:
        Result nextResult = result.then doubleFunction

        then:
        nextResult.value.get() == 4
    }

    void 'get fail result and later try do something'() {
        given:
        Error error = Stub(Error)
        Result result = Result.fail(error)

        when:
        Result nextResult = result.then doubleFunction

        then:
        nextResult.hasErrors()
        nextResult.errors.get() == [error]
    }

    void 'then async'() {
        given:
        Result result = Result.success(5)
        AtomicInteger count = new AtomicInteger(0)

        when:
        FutureResult futureResult = result.thenAsync { successValue ->
            sleep(10)
            assert count.getAndIncrement() == 0
            Result.success(successValue * 2)
        }

        then:
        count.get() == 0

        when:
        Result afterResult = futureResult.get()

        then:
        afterResult.value.get() == 10
        count.get() == 1
    }

    private Function<Integer, Result<Integer>> getDoubleFunction() {
        { successValue -> Result.success(successValue * 2) }
    }
}
