package es.osoco.monkfish;

public class TestError implements Error<String> {

    private final String error;

    public TestError(String error) {
        this.error = error;
    }

    @Override
    public String getCode() {
        return null;
    }
}
