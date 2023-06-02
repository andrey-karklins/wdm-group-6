package app;

import io.javalin.http.HttpResponseException;

import java.util.Collections;

public class StockError extends HttpResponseException {
    public StockError(String message) {
        super(404, message, Collections.emptyMap());
    }
}
