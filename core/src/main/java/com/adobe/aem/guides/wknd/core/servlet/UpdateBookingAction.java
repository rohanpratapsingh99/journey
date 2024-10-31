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
	        "sling.servlet.paths=/bin/updateAction"    }
	)
public class UpdateBookingAction extends SlingAllMethodsServlet {

    private static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(UpdateBookingAction.class);

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private final Gson gson = new Gson();


    @Override
    protected void doPost(final SlingHttpServletRequest request,
                          final SlingHttpServletResponse response) throws ServletException, IOException {
        JsonObject requestData = new Gson().fromJson(request.getReader(), JsonObject.class);
        String username = requestData.get("username").getAsString();
        String bookingId = requestData.get("bookingId").getAsString();
        String actionType = requestData.get("actionType").getAsString();
        String notes = requestData.get("notes").getAsString();
        ResourceResolver resourceResolver =null;
// Establish resolver
        Map<String, Object> authInfo = new HashMap<>();
        authInfo.put(ResourceResolverFactory.SUBSERVICE, "dataWriteService");
        try {
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(authInfo);
        } catch (LoginException e) {
            throw new RuntimeException(e);
        }
        // Path to the booking resource
        String bookingPath = "/etc/bookings/" + username + "/" + bookingId;

        Resource bookingResource = resourceResolver.getResource(bookingPath);
        if (bookingResource == null) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("Booking not found.");
            return;
        }

        try {
            // Prepare properties to update
            Map<String, Object> propertiesToUpdate = new HashMap<>();
            propertiesToUpdate.put("notes", notes);

            // Handle action type
            switch (actionType.toLowerCase()) {
                case "rejected":
                    propertiesToUpdate.put("actionType", "Rejected");
                    break;
                case "approved":
                    propertiesToUpdate.put("actionType", "Approved");
                    break;
                // Add more action types as necessary
                default:
                    response.setStatus(SlingHttpServletResponse.SC_BAD_REQUEST);
                    response.getWriter().write("Invalid action type.");
                    return;
            }

            // Update properties on the booking node
            ValueMap bookingProperties = bookingResource.adaptTo(ValueMap.class);
            if (bookingProperties != null) {
                // Update existing properties or add new ones
                for (Map.Entry<String, Object> entry : propertiesToUpdate.entrySet()) {
                    bookingResource.adaptTo(ModifiableValueMap.class).put(entry.getKey(), entry.getValue());
                }
            }

            // Commit changes
            resourceResolver.commit();

            response.setStatus(SlingHttpServletResponse.SC_OK);
            response.getWriter().write("Booking updated successfully.");

        } catch (Exception e) {
            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("Error updating booking: " + e.getMessage());
        } finally {
            // Ensure resource resolver is closed if needed
            if (resourceResolver != null && resourceResolver.isLive()) {
                resourceResolver.close();
            }
        }
    }
}