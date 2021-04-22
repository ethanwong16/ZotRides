package com.example.zotrides;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
/**
 * This ShoppingCartServlet is declared in the web annotation below,
 * which is mapped to the URL pattern /api/index.
 */
@WebServlet(name = "ShoppingCartServlet", urlPatterns = "/api/shopping-cart")
public class ShoppingCartServlet extends HttpServlet {

    // Create a dataSource which registered in web.xml
    private DataSource dataSource;

    public void init(ServletConfig config) {
        try {
            dataSource = (DataSource) new InitialContext().lookup("java:comp/env/jdbc/zotrides");
        } catch (NamingException e) {
            e.printStackTrace();
        }
    }

    /**
     * handles GET requests to view shopping cart
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        HttpSession session = request.getSession();

        JsonObject responseJsonObject = new JsonObject();

        ArrayList<CartItem> previousItems = (ArrayList<CartItem>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new ArrayList<>();
        }
        JsonArray previousItemsJsonArray = new JsonArray();

        for (CartItem item : previousItems) {
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("name", item.getCarName());
            jsonObject.addProperty("pickupLocation", item.getPickupLocation());
            jsonObject.addProperty("startDate", item.getStartDate());
            jsonObject.addProperty("endDate", item.getEndDate());
            jsonObject.addProperty("unitPrice", item.getUnitPrice());
            jsonObject.addProperty("id", item.getItemID()); // used to uniquely identify cart item
            previousItemsJsonArray.add(jsonObject);
        }

        responseJsonObject.add("previousItems", previousItemsJsonArray);

        // write all the data into the jsonObject
        response.getWriter().write(responseJsonObject.toString());
    }

    /**
     * handles POST requests to add items to shopping cart
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // get elements of item to store into shopping cart
        String name = request.getParameter("name");
        String carID = request.getParameter("carID");
        String pickupLocation = request.getParameter("pickupLocation");
        String startDate = request.getParameter("startDate");
        String endDate = request.getParameter("endDate");
        int unitPrice = Integer.parseInt(request.getParameter("unitPrice"));
        System.out.println("name: " + name + " pickupLocation: " + pickupLocation + " dates: " + startDate + " " + endDate
            + " unitPrice: " + unitPrice);

        // TODO: store customerID in LoginServlet to session
        HttpSession session = request.getSession();

        // get the previous items in a ArrayList
        ArrayList<CartItem> previousItems = (ArrayList<CartItem>) session.getAttribute("previousItems");
        if (previousItems == null) {
            previousItems = new ArrayList<>();
            previousItems.add(new CartItem(name, carID, pickupLocation, startDate, endDate, unitPrice));
            session.setAttribute("previousItems", previousItems);
        } else {
            // prevent corrupted states through sharing under multi-threads
            // will only be executed by one thread at a time
            synchronized (previousItems) {
                previousItems.add(new CartItem(name, carID, pickupLocation, startDate, endDate, unitPrice));
            }
        }

        try (Connection conn = dataSource.getConnection()) {

            // Declare our statement
            Statement statement = conn.createStatement();

            String query = "";
            for (CartItem item : previousItems) {
                // TODO : need to access customer ID
                String customerID = request.getSession().getAttribute("customerID").toString();
                query += item.toQuery(customerID);
            }

            // Perform the query
            int numLinesEdited = statement.executeUpdate(query);

            // set response status to 200 (OK)
            response.setStatus(200);
            statement.close();

        } catch (Exception e) {

            // write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("status", "fail");
            jsonObject.addProperty("message", e.getMessage());
            response.getWriter().write(jsonObject.toString());

            // set response status to 500 (Internal Server Error)
            response.setStatus(500);

        }
        JsonObject responseJsonObject = new JsonObject();
        responseJsonObject.addProperty("status", "success");
        responseJsonObject.addProperty("message", "success");
        response.getWriter().write(responseJsonObject.toString());
    }
}