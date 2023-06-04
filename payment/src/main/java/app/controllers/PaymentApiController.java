package app.controllers;

import app.PaymentError;
import app.models.User;
import app.services.PaymentService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;


public class PaymentApiController {

    public static final ConcurrentHashMap<UUID, CompletableFuture<String>> transactionMap = new ConcurrentHashMap<>();
    private static final ObjectMapper mapper = new ObjectMapper();
    private final String orderUrl = "http://order-service:5000";


    /**
     * Returns funds back to the user. If method fails, the event ReturnFundsFailed is sent to reverse operations
     * in the other microservices. Completes the CompletableFuture started by calling the endpoint cancel.
     * @param userID the UUID of the user
     * @param orderID the UUID of the order
     * @param transactionID the UUID of the transaction
     * @param totalCost the total cost of the order
     * @param items the list of items in the order
     * @throws JsonProcessingException
     */
    public static void returnFunds(UUID userID, UUID orderID, UUID transactionID, int totalCost, List<UUID> items) throws JsonProcessingException {
        try {
            PaymentService.addFunds(userID, totalCost);
            transactionMap.get(transactionID).complete("Success");
        } catch (Exception e) {
            String err = "Adding funds back failed due to " + e;
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("UserID", userID);
            eventData.put("OrderID", orderID);
            eventData.put("TransactionID", transactionID);
            eventData.put("ErrorMsg", err);
            eventData.put("Items", items);
            PaymentService.sendEvent("ReturnFundsFailed", mapper.writeValueAsString(eventData));
        }

    }

    /**
     * Method used to subtract the funds of a user, when the checkout endpoint is called.
     * If method fails, the event FundsSubtractFailed is sent to reverse operations
     * @param userID the UUID of the user
     * @param orderID the UUID of the order
     * @param transactionID the UUID of the transaction
     * @param totalCost the total cost of the order
     * @param items the list of items in the order
     * @throws JsonProcessingException
     */
    public static void subtractFunds(UUID userID, UUID orderID, UUID transactionID, int totalCost, List<UUID> items) throws JsonProcessingException {
        try {
            User user = PaymentService.findUserById(userID);
            if (user.credit < totalCost) {
                throw new PaymentError("Not enough credit");
            }
            PaymentService.addFunds(userID, -1 * totalCost);

            Map<String, Object> eventData = new HashMap<>();
            eventData.put("OrderID", orderID);
            eventData.put("TransactionID", transactionID);
            eventData.put("UserID", userID);
            eventData.put("Items", items);
            eventData.put("TotalCost", totalCost);
            PaymentService.sendEvent("FundsSubtracted", mapper.writeValueAsString(eventData));

        } catch (Exception e) {
            String err = "Subtracting funds failed due to " + e;
            Map<String, Object> eventData = new HashMap<>();
            eventData.put("OrderID", orderID);
            eventData.put("TransactionID", transactionID);
            eventData.put("ErrorMsg", err);
            eventData.put("Items", items);
            PaymentService.sendEvent("FundsSubtractFailed", mapper.writeValueAsString(eventData));
        }
    }

    /**
     * Method called when a user wants to cancel an order. We initialize a new transactionID and a Completable Future
     * object. We send the event CancelPayment, so that other microservices are informed. The final result is returned
     * once all microservices successfully complete the canceling process.
     * @param userID the UUID of the user
     * @param orderID the UUID of the order
     * @return Success, if the order has been canceled. Otherwise, throws an exception which returns a 404 Response
     * @throws ExecutionException
     * @throws InterruptedException
     * @throws JsonProcessingException
     */
    public String cancel(String userID, String orderID) throws ExecutionException, InterruptedException, JsonProcessingException {
        UUID transactionID = UUID.randomUUID();
        CompletableFuture<String> future = new CompletableFuture<>();
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("UserID", userID);
        eventData.put("OrderID", orderID);
        eventData.put("TransactionID", transactionID);
        transactionMap.put(transactionID, future);
        PaymentService.sendEvent("CancelPayment", mapper.writeValueAsString(eventData));
        String result = future.get();
        transactionMap.remove(transactionID);
        if (!Objects.equals(result, "Success")) {
            throw new PaymentError(result);
        }
        return result;

    }

    /**
     * Method called when the pay/{user_id}/{order_id}/{amount} endpoint is accessed. Normally, paying should happen
     * when a user calls the /checkout/ endpoint in the order microservice. Therefore, we do not check if the
     * order is real. We only check if the amount that needs to be paid is greater than the money that the user has.
     * @param userID the UUID of the user
     * @param orderID the UUID of the order
     * @param amount the amount that needs to be paid
     * @return Success or Fail depending on whether the payment went through or not
     */
    public String pay(String userID, String orderID, int amount) {
        User user = PaymentService.findUserById(UUID.fromString(userID));
        if (user.credit < amount) {
            throw new PaymentError("Not Enough Credit");
        }

        boolean status = PaymentService.addFunds(UUID.fromString(userID), -1 * amount);
        if (status) {
            PaymentService.sendEvent("PaymentSucceeded", orderID);
            return "Payment went through.";
        }

        return "Payment failed";
    }

    /**
     * Method called when the status of an order is called. As the status of the order is available in the order microservice
     * we send an HTTP request to fetch the order object.
     * @param userID the UUID of the user
     * @param orderID the UUID of the order
     * @return JSON containing a boolean field paid, which represents the status of the order
     */
    public Map<String, Object> status(String userID, String orderID) {
        Map<String, Object> res = new HashMap<>(Map.of("status", true));
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(orderUrl + "/find/" + orderID);
            String response = client.execute(request, httpResponse -> EntityUtils.toString(httpResponse.getEntity()));

            JsonNode responseJSON = mapper.readTree(response);
            String receivedUserId = responseJSON.get("user_id").asText();
            boolean paid = responseJSON.get("paid").asBoolean();

            if (receivedUserId.equals(userID)) {
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
        } else throw new PaymentError("User not found");
        return res;

    }
}
