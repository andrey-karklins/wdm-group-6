package app.controllers;

import app.PaymentError;
import app.models.User;
import app.services.PaymentService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PaymentApiController {

    private final String orderUrl = "http://order-service:5000";

    private final ObjectMapper mapper = new ObjectMapper();

    public String pay(String userId, String orderId, float amount) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet request = new HttpGet(orderUrl + "/find/" + orderId);
            String response = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));


            JsonNode responseJSON = mapper.readTree(response);
            String receivedUserId = responseJSON.get("user_id").asText();

            if (!receivedUserId.equals(userId)) {
                return "userID does not correspond to the one in orderID";
            }

            User user = PaymentService.findUserById(UUID.fromString(userId));
            if (user.credit < amount) {
                return "Not enough Credit";
            }

            //Should the endpoint use the cost in the order instance or the amount provided by the endpoint?
            float cost = (float) responseJSON.get("total_cost").asDouble();

            if (cost != amount) {
                return "Wrong Amount to pay for the order.";
            }

            boolean status = PaymentService.addFunds(UUID.fromString(userId), -1*amount);
            if (status) {
                PaymentService.sendEvent("PaymentSucceeded", orderId);
                return "Payment went through.";
            }


        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Payment failed";
    }

    public String cancel(String userId, String orderId) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(orderUrl + "/find/" + orderId);
            String response = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));

            JsonNode responseJSON = mapper.readTree(response);
            String receivedUserId = responseJSON.get("user_id").asText();
            boolean orderStatus =  responseJSON.get("paid").asBoolean();

            if (!receivedUserId.equals(userId)) {
                return "userID does not correspond to the one in orderID";
            }

            if (!orderStatus) {
                return "The order has not been paid yet";
            }

            float cost = (float) responseJSON.get("total_cost").asDouble();
            PaymentService.addFunds(UUID.fromString(userId), cost);
            PaymentService.sendEvent("OrderCanceled", responseJSON.toString());

        } catch (IOException e) {
            e.printStackTrace();
        }

        return "Success";

    }

    public Map<String, Object> status(String userId, String orderId) {
        Map<String, Object> res = new HashMap<>(Map.of("status", true));
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(orderUrl + "/find/" + orderId);
            String response = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));

            JsonNode responseJSON = mapper.readTree(response);
            String receivedUserId = responseJSON.get("user_id").asText();
            boolean paid = responseJSON.get("paid").asBoolean();

            if(receivedUserId.equals(userId)) {
                res.put("paid", paid);
                return res;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public Map<String, Object> addFunds(String userId, float amount) {
        Map<String, Object> res = new HashMap<>(Map.of("paid", true));
        boolean success = PaymentService.addFunds(UUID.fromString(userId), amount);
        res.put("paid", success);
        return res;
    }

    public Map<String, Object> createUser() {
        Map<String, Object> res = new HashMap<>(Map.of("user_id", true));
        UUID user_id = PaymentService.createUser();
        res.put("user_id", user_id.toString());
        return res;
    }

    public Map<String, Object> findUser(String userId) {
        Map<String, Object> res = new HashMap<>();
        User user = PaymentService.findUserById(UUID.fromString(userId));

        if (user != null) {
             res.put("userId", user.user_id.toString());
             res.put("credit", user.credit);
            }
        else throw new PaymentError("User not found");
        return res;

    }
}
