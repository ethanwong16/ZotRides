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


// Declaring a WebServlet called BrowseServlet, which maps to url "/api/browse-car"
@WebServlet(name = "BrowseServlet", urlPatterns = "/api/browse-car")
public class BrowseServlet extends HttpServlet {
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
        response.setContentType("application/json"); // Response mime type

        // Retrieve parameter id from url request.
        String category = request.getParameter("category");
        String yearDigit = request.getParameter("year");
        String modelLetter = request.getParameter("model");


        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        try (Connection conn = dataSource.getConnection()) {
            // construct query
            String additional = "";
            if (category != null && !category.isEmpty()) {
                int catID = 8;
                switch (category) {
                    case "pickup":
                        catID = 1;
                        break;
                    case "wagon":
                        catID = 2;
                        break;
                    case "van":
                        catID = 3;
                        break;
                    case "coupe":
                        catID = 4;
                        break;
                    case "sedan":
                        catID = 5;
                        break;
                    case "hatchback":
                        catID = 6;
                        break;
                    case "convertible":
                        catID = 7;
                    default: // SUV
                        break;
                }
                additional += " AND categoryID = " + catID;
            }
            if (modelLetter != null && !modelLetter.isEmpty())
                additional += " AND model LIKE \"" + modelLetter + "%\"";
            if (yearDigit != null && !yearDigit.isEmpty())
                additional += " AND year LIKE \"" + yearDigit + "%\"";

            String query = "SELECT Cars.id as id, group_concat(DISTINCT concat_ws(' ', make, model, year)) as name, \n" +
                    "\t\tname as category, rating, numVotes,\n" +
                    "        group_concat(DISTINCT address ORDER BY pickupLocationID SEPARATOR ';') as address, \n" +
                    "        group_concat(DISTINCT phoneNumber ORDER BY pickupLocationID SEPARATOR ';') as phoneNumber,\n" +
                    "        group_concat(DISTINCT pickupLocationID ORDER BY pickupLocationID SEPARATOR ';') as pickupID\n" +
                    "FROM category_of_car, Category, Cars, Ratings, pickup_car_from, PickupLocation\n" +
                    "WHERE category_of_car.categoryID = Category.id AND category_of_car.carID = Cars.id" + additional + " \n" +
                    "\tAND Ratings.carID = Cars.id AND pickup_car_from.carID = Cars.id AND pickup_car_from.pickupLocationID = PickupLocation.id\n" +
                    "GROUP BY Cars.id\n" +
                    "ORDER BY rating DESC\n"+
                    "LIMIT 100;";

            System.out.println("query:\n" + query);

            // Declare our statement
            PreparedStatement statement = conn.prepareStatement(query);

            // Perform the query
            ResultSet rs = statement.executeQuery();

            JsonArray jsonArray = new JsonArray();

            // Iterate through each row of rs
            int count = 0;
            while (rs.next() && count++ < 20) {
                String car_id = rs.getString("id");
                String car_name = rs.getString("name");
                String car_category = rs.getString("category");
                double car_rating = rs.getDouble("rating");
                int car_votes = rs.getInt("numVotes");
                String location_address = rs.getString("address");
                String location_phone = rs.getString("phoneNumber");
                String location_ids = rs.getString("pickupID");
                /* NOTE : unlike previous example, we are wrapping everything into JSON to return it
                while previous example returned HTML.  Now HTML is generated by front-end
                 */

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("car_id", car_id);
                jsonObject.addProperty("car_name", car_name);
                jsonObject.addProperty("car_category", car_category);
                jsonObject.addProperty("car_rating", car_rating);
                jsonObject.addProperty("car_votes", car_votes);
                jsonObject.addProperty("location_address", location_address);
                jsonObject.addProperty("location_phone", location_phone);
                jsonObject.addProperty("location_ids", location_ids);
                System.out.println(jsonObject.toString());
                jsonArray.add(jsonObject);
            }

            // write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);

            rs.close();
            statement.close();

        } catch (Exception e) {
            // write error message JSON object to output
            JsonObject jsonObject = new JsonObject();
            jsonObject.addProperty("errorMessage", e.getMessage());
            out.write(jsonObject.toString());

            // set response status to 500 (Internal Server Error)
            response.setStatus(500);
        } finally {
            out.close();
        }


    }

}