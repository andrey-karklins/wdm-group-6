package controllers;

import java.util.Map;

public class PaymentApiController {
    public String pay(String userId, String orderId, int amount) {
        return "Success";
    }

    public String cancel(String userId, String orderId) {
        return "Success";
    }

    public Map<String, Object> status(String userId, String orderId) {
        Map<String, Object> res = Map.of("status", true);
        return res;
    }

    public String addFunds(String userId, int amount) {
        return "Success";
    }

    public String createUser() {
        return "Success";
    }

    public Map<String, Object> findUser(String userId) {
        Map<String, Object> res = Map.of("userId", userId, "credit", 100);
        return res;
    }
}
