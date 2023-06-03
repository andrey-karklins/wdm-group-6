package app;

import io.javalin.http.HttpResponseException;

import java.util.Collections;

public class OrderError extends HttpResponseException {
    public OrderError(String message) {
        super(404, message, Collections.emptyMap());
    }
}
