package com.adobe.aem.guides.wknd.core.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet that fetches booking data for users.
 */
@Component(
        service = Servlet.class,
        property = {
                "sling.servlet.methods=GET",
                "sling.servlet.paths=/bin/fetch_booking"
        }
)
public class FetchBookingServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(FetchBookingServlet.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private static final String BOOKINGS_PATH = "/etc/bookings";

    private final Gson gson = new Gson();

    @Override
    protected void doGet(final SlingHttpServletRequest request,
                         final SlingHttpServletResponse response) throws ServletException, IOException {
        String userId = request.getParameter("id");
        JsonArray usersJsonArray = new JsonArray();
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "dataWriteService");
        ResourceResolver resolver = null;

        try {
            resolver = resourceResolverFactory.getServiceResourceResolver(authInfo);
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }

        // Fetch bookings based on userId parameter
        if (userId != null && !userId.isEmpty()) {
            fetchBookingsByUser(userId, usersJsonArray, resolver);
        } else {
            fetchAllBookings(usersJsonArray, resolver);
        }

        response.setContentType("application/json");
        JsonObject responseJson = new JsonObject();
        responseJson.add("users", usersJsonArray);
        response.getWriter().write(gson.toJson(responseJson));
    }

    private void fetchAllBookings(JsonArray usersJsonArray, ResourceResolver resourceResolver) {
        Resource bookingsRoot = resourceResolver.getResource(BOOKINGS_PATH);
        if (bookingsRoot == null) {
            return; // No bookings found
        }

        // Iterate through users
        for (Resource user : bookingsRoot.getChildren()) {
            JsonObject userJson = new JsonObject();
            String userName = user.getName();
            JsonArray userBookingsArray = new JsonArray();
            if ("rep:policy".equals(userName)) {
                continue; // Skip rep:policy users
            }
            for (Resource booking : user.getChildren()) {
                JsonObject bookingJson = createBookingJson(booking);
                userBookingsArray.add(bookingJson);
            }

            userJson.addProperty("username", userName);
            userJson.add("bookings", userBookingsArray);
            usersJsonArray.add(userJson);
        }
    }

    private void fetchBookingsByUser(String userId, JsonArray usersJsonArray, ResourceResolver resourceResolver) {
        Resource userResource = resourceResolver.getResource(BOOKINGS_PATH + "/" + userId);
        if (userResource == null || "rep:policy".equals(userId)) {
            return; // User not found
        }

        JsonObject userJson = new JsonObject();
        userJson.addProperty("username", userId);
        JsonArray userBookingsArray = new JsonArray();

        for (Resource booking : userResource.getChildren()) {
            JsonObject bookingJson = createBookingJson(booking);
            userBookingsArray.add(bookingJson);
        }

        userJson.add("bookings", userBookingsArray);
        usersJsonArray.add(userJson);
    }

    private JsonObject createBookingJson(Resource booking) {
        JsonObject bookingJson = new JsonObject();
        bookingJson.addProperty("bookingId", booking.getName());

        // Add all properties of the booking
        ValueMap properties = booking.adaptTo(ValueMap.class);
        for (String prop : properties.keySet()) {
            if (!"jcr:primaryType".equals(prop)) { // Exclude primaryType if present
                bookingJson.addProperty(prop, properties.get(prop, String.class));
            }
        }

        // Add travelers
        if (properties.containsKey("travellers")) {
            String travellersJsonString = properties.get("travellers", String.class);

            // Parse the JSON string into a JsonArray
            JsonArray travellersArray = gson.fromJson(travellersJsonString, JsonArray.class);

            // Create a StringBuilder to store comma-separated full names
            StringBuilder fullNameList = new StringBuilder();

            // Loop through each traveller in the array
            if (travellersArray != null) {
                for (JsonElement travellerElement : travellersArray) {
                    JsonObject traveller = travellerElement.getAsJsonObject();
                    String fullName = traveller.get("fullName").getAsString();

                    // Append the full name to the StringBuilder, with comma separation
                    if (fullNameList.length() > 0) {
                        fullNameList.append(", ");
                    }
                    fullNameList.append(fullName);
                }
            }

            // Print the result to check output
            //System.out.println("Travellers: " + fullNameList.toString());

            // Update the `bookingJson` object to include the comma-separated names
            bookingJson.addProperty("travellers", fullNameList.toString());
            //bookingJson.add("travellers", gson.fromJson(properties.get("travellers", String.class), JsonArray.class));
        }

        return bookingJson;
    }
}
