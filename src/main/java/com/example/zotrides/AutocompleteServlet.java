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
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

// Declaring a WebServlet called AutocompleteServlet, which maps to url "/api/autocomplete"
@WebServlet(name = "AutocompleteServlet", urlPatterns = "/api/autocomplete")
public class AutocompleteServlet extends HttpServlet {
    private static final long serialVersionUID = 2L;

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
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
     *      response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    /**
     * handles POST requests to store query in session
     */
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        /* API
         *   - Input : entire string that user entered on search form
         *   - Output : numResults = number of results
         *              results : JSONArray containing results
         *                  each result has car_id and car_name (make/model/year)
         *          {numResults:..., results:[{car_id:..., car_name:...}, ...]} */

        PrintWriter out = response.getWriter();

        // Retrieve parameters from url request
        String token = request.getParameter("token");

        String additional = "";
        if (token == null || token.trim().isEmpty()) {
            // no autocomplete results
            JsonObject result = new JsonObject();
            result.addProperty("numResults", 0);
            result.add("results", new JsonArray());
            out.write(result.toString());
            out.close();
            return;
        }

        String[] tokens = token.trim().split("\\s+");
        for (String curr : tokens) {
            additional += "+" + curr + "*";
        }

        System.out.println("tokens: " + additional);

        int fuzzyThreshold = token.trim().length() / 3;
        String query = "SELECT id, CONCAT_WS(' ', make, model, year) as name FROM Cars\n" +
                "WHERE (MATCH(model) AGAINST (? IN BOOLEAN MODE) OR edth(model, ?, "+ fuzzyThreshold + "))\n" + //TODO: added edth for fuzzy
                "LIMIT 10;";

        // without fuzzy
        /*
        String query = "SELECT id, CONCAT_WS(' ', make, model, year) as name FROM Cars\n" +
                "WHERE MATCH(model) AGAINST (? IN BOOLEAN MODE)\n" +
                "LIMIT 10;";*/


        // Run query & return response
        response.setContentType("application/json"); // Response mime type
        try (Connection conn = dataSource.getConnection()) {
            // run query
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, additional);
            statement.setString(2, token.trim()); // TODO : ADDED THIS LINE

            System.out.println("query:\n" + statement.toString() + "\n");
            ResultSet rs = statement.executeQuery();

            // process results
            JsonArray jsonArray = new JsonArray();
            int count = 0;
            while (rs.next() && count++ < 10) { // Iterate through each row of rs
                String car_id = rs.getString("id");
                String car_name = rs.getString("name");

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("car_id", car_id);
                jsonObject.addProperty("car_name", car_name);
                jsonArray.add(jsonObject);
            }

//            System.out.println(jsonArray);

            JsonObject result = new JsonObject();
            result.addProperty("numResults", count);
            result.add("results", jsonArray);
            out.write(result.toString());
            response.setStatus(200);         // set response status to 200 (OK)
            rs.close();
            statement.close();

            System.out.println("no issues");

        } catch (Exception e) {
            // write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // display on backend (more detailed)
            System.out.println("error: " + e.getMessage());

            // set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }
    }
}