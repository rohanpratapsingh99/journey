/*
 *  Copyright 2015 Adobe Systems Incorporated
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.adobe.aem.guides.wknd.core.servlet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

/**
 * Servlet that writes some sample content into the response. It is mounted for
 * all resources of a specific Sling resource type. The
 * {@link SlingSafeMethodsServlet} shall be used for HTTP methods that are
 * idempotent. For write operations use the {@link SlingAllMethodsServlet}.
 */
@Component(
	    service = Servlet.class,
	    property = {
	        "sling.servlet.methods=POST",
	        "sling.servlet.paths=/bin/booking"    }
	)
public class BookingApi extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(BookingApi.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;



    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws ServletException, IOException {
        ResourceResolver resolver = null;
        try {
            // Retrieve JSON input
            JsonObject bookingData = new Gson().fromJson(request.getReader(), JsonObject.class);

            // Extract fields
            String username = bookingData.get("username").getAsString();
            String adventureTitle = bookingData.get("adventureTitle").getAsString();
            String pricePerPerson = bookingData.get("pricePerPerson").getAsString();
            String adventureId = bookingData.get("adventureId").getAsString();
            String totalAmount = bookingData.get("totalAmount").getAsString();
            String transactionNumber = bookingData.get("transactionNumber").getAsString();
            String bookingDate = bookingData.get("transactionDate").getAsString();
            String travelDate = bookingData.get("bookingDate").getAsString();

            // Date format for storage (supports both 'yyyy-MM-dd' and 'yyyy-MM-dd'T'HH:mm:ss'Z')
            SimpleDateFormat inputDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            SimpleDateFormat alternativeDateFormat = new SimpleDateFormat("yyyy-MM-dd");
            SimpleDateFormat outputDateFormat = new SimpleDateFormat("dd-MMMM-yyyy");

            String formattedBookingDate, formattedTravelDate;
            try {
                // Try parsing with full timestamp first
                formattedBookingDate = outputDateFormat.format(inputDateFormat.parse(bookingDate));
            } catch (ParseException e) {
                // Fall back to parsing as just the date if timestamp is missing
                formattedBookingDate = outputDateFormat.format(alternativeDateFormat.parse(bookingDate));
            }

// Similar parsing for travelDate
            try {
                formattedTravelDate = outputDateFormat.format(inputDateFormat.parse(travelDate + "T00:00:00Z"));
            } catch (ParseException e) {
                formattedTravelDate = outputDateFormat.format(alternativeDateFormat.parse(travelDate));
            }
            // Generate Booking ID
            String bookingId = username.substring(0, 2).toUpperCase() + RandomStringUtils.randomNumeric(5);

            // Establish resolver
            Map<String, Object> authInfo = new HashMap<>();
            authInfo.put(ResourceResolverFactory.SUBSERVICE, "dataWriteService");
            resolver = resourceResolverFactory.getServiceResourceResolver(authInfo);

            // Prepare paths and save booking data
            Resource bookingsRoot = getOrCreateResource(resolver, "/etc/bookings", "sling:Folder", resolver.getResource("/etc"));
            LOG.info("DATA Resolver --> {}", resolver);

            Resource userNode = getOrCreateResource(resolver, "/etc/bookings/" + username, "sling:Folder", bookingsRoot);
            LOG.info("DATA Resource --> {}", userNode);

            // Create the booking node under the user node
            Resource bookingNode = getOrCreateResource(resolver, userNode.getPath() + "/" + bookingId, "nt:unstructured", userNode);

            String actionType= StringUtils.EMPTY;
            String notes= StringUtils.EMPTY;
            // Prepare booking properties
            Map<String, Object> bookingProps = new HashMap<>();
            bookingProps.put("adventureTitle", adventureTitle);
            bookingProps.put("pricePerPerson", pricePerPerson);
            bookingProps.put("adventureId", adventureId);
            bookingProps.put("totalAmount", totalAmount);
            bookingProps.put("transactionNumber", transactionNumber);
            bookingProps.put("bookingDate", formattedBookingDate);
            bookingProps.put("travelDate", formattedTravelDate);
            bookingProps.put("actionType", actionType);
            bookingProps.put("notes", notes);
            bookingProps.put("travellers", new Gson().toJson(bookingData.get("travellers")));

            // Store the booking properties in the booking node
            ModifiableValueMap modifiableValueMap = bookingNode.adaptTo(ModifiableValueMap.class);
            if (modifiableValueMap != null) {
                modifiableValueMap.putAll(bookingProps);
            } else {
                response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                response.getWriter().write("Error adapting booking node to ModifiableValueMap.");
                return;
            }

            // Commit changes
            resolver.commit();
            response.setStatus(SlingHttpServletResponse.SC_CREATED);
            response.getWriter().write("Booking successfully added with ID: " + bookingId);

        } catch (JsonParseException | ParseException e) {
            response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("Invalid JSON format.");
        } catch (PersistenceException | LoginException e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error saving booking: " + e.getMessage());
        } finally {
            if (resolver != null && resolver.isLive()) {
                resolver.close();
            }
        }
    }

    public static Resource getOrCreateResource(ResourceResolver resolver, String path, String primaryType, Resource parentResource)
            throws PersistenceException {
        Resource resource = resolver.getResource(path);
        if (resource != null) {
            return resource;
        }

        // Get the last segment of the path
        String[] pathSegments = path.split("/");
        String nodeName = pathSegments[pathSegments.length - 1];

        // Log parent resource details before attempting to create
        LOG.info("Attempting to create resource '{}' under parent '{}'", nodeName, parentResource.getPath());

        Map<String, Object> properties = new HashMap<>();
        properties.put("jcr:primaryType", primaryType);

        try {
            // Create the resource and return it
            return resolver.create(parentResource, nodeName, properties);
        } catch (PersistenceException e) {
            LOG.error("Failed to create resource '{}' under parent '{}': {}", nodeName, parentResource.getPath(), e.getMessage(), e);
            throw e;
        }
    }
}