package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import models.User;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import services.PaymentService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class PaymentApiController {

    private final String orderUrl = "http://order-service:5000";

    public String pay(String userId, String orderId, int amount) {

        try (CloseableHttpClient client = HttpClients.createDefault()) {

            HttpGet request = new HttpGet(orderUrl + "/find/" + orderId);
            String response = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));

            ObjectMapper mapper = new ObjectMapper();
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
            int cost = responseJSON.get("total_cost").asInt();

            boolean status = PaymentService.addFunds(UUID.fromString(userId), -1*amount);
            if (status) {
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

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJSON = mapper.readTree(response);
            String receivedUserId = responseJSON.get("user_id").asText();

            if (!receivedUserId.equals(userId)) {
                return "userID does not correspond to the one in orderID";
            }

            int cost = responseJSON.get("total_cost").asInt();
            PaymentService.addFunds(UUID.fromString(userId), cost);

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

            ObjectMapper mapper = new ObjectMapper();
            JsonNode responseJSON = mapper.readTree(response);
            String receivedUserId = responseJSON.get("user_id").asText();
            Boolean paid = responseJSON.get("paid").asBoolean();

            if(receivedUserId.equals(userId)) {
                res.put("paid", paid);
                return res;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return res;
    }

    public Map<String, Object> addFunds(String userId, int amount) {
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
        return res;
    }
}
