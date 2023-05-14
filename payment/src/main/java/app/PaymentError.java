package app;

import io.javalin.http.HttpResponseException;

import java.util.Collections;

public class PaymentError extends HttpResponseException {
    public PaymentError(String message) {
        super(404, message, Collections.emptyMap());
    }
}
