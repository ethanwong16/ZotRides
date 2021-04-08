package com.example.zotrides;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import javax.annotation.Resource;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;


// Declaring a WebServlet called StarsServlet, which maps to url "/api/stars"
@WebServlet(name = "CarsServlet", urlPatterns = "/api/cars")
public class CarsServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    // Create a dataSource which registered in web.xml
    @Resource(name = "jdbc/zotrides")
    private DataSource dataSource;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json"); // Response mime type

        // Output stream to STDOUT
        PrintWriter out = response.getWriter();

        try {
            // Get a connection from dataSource
            Connection dbcon = dataSource.getConnection();

            // Declare our statement
            Statement statement = dbcon.createStatement();

            /*String query = "WITH car_info(id, model, make, year, name, rating, numVotes) AS\n" +
                    "\t(SELECT Cars.id, model, make, year, name, rating, numVotes\n" +
                    "\tFROM (Cars, category_of_car, Category) LEFT OUTER JOIN Ratings ON Ratings.carID = Cars.id\n" +
                    "\tWHERE Cars.id = category_of_car.carID \n" +
                    "\t\t\tAND category_of_car.categoryID = Category.id),\n" +
                    "\n" +
                    "numbered_pickup(carID, pickupLocationID, num) AS\n" +
                    "\t(SELECT carID, pickupLocationID, ROW_NUMBER() OVER(PARTITION BY carID) \n" +
                    "\tFROM pickup_car_from)\n" +
                    "\n" +
                    "SELECT car_info.id as id, group_concat(DISTINCT concat_ws(' ', make, model, year)) as name, \n" +
                    "\t\tname as category, rating, numVotes,\n" +
                    "        group_concat(DISTINCT address SEPARATOR ';') as address, \n" +
                    "        group_concat(DISTINCT phoneNumber SEPARATOR ';') as phoneNumber,\n" +
                    "        group_concat(DISTINCT pickupLocationID SEPARATOR ';') as pickupID\n" +
                    "FROM (car_info LEFT OUTER JOIN numbered_pickup ON car_info.id = numbered_pickup.carID)\n" +
                    "\t\t\tLEFT OUTER JOIN PickupLocation ON numbered_pickup.pickupLocationID = PickupLocation.id AND numbered_pickup.num < 4\n" +
                    "GROUP BY car_info.id\n" +
                    "ORDER BY rating DESC; ";*/

            String query = "WITH car_info(id, model, make, year, name, rating, numVotes) AS\n" +
                    "\t(SELECT Cars.id, model, make, year, name, rating, numVotes\n" +
                    "\tFROM (Cars, category_of_car, Category) LEFT OUTER JOIN Ratings ON Ratings.carID = Cars.id\n" +
                    "\tWHERE Cars.id = category_of_car.carID \n" +
                    "\t\t\tAND category_of_car.categoryID = Category.id),\n" +
                    "\n" +
                    "numbered_pickup(carID, pickupLocationID, num) AS\n" +
                    "\t(SELECT carID, pickupLocationID, ROW_NUMBER() OVER(PARTITION BY carID) \n" +
                    "\tFROM pickup_car_from)\n" +
                    "\n" +
                    "SELECT car_info.id as id, group_concat(DISTINCT concat_ws(' ', make, model, year)) as name, \n" +
                    "\t\tname as category, rating, numVotes,\n" +
                    "        group_concat(DISTINCT address ORDER BY pickupLocationID SEPARATOR ';') as address, \n" +
                    "        group_concat(DISTINCT phoneNumber ORDER BY pickupLocationID SEPARATOR ';') as phoneNumber,\n" +
                    "        group_concat(DISTINCT pickupLocationID ORDER BY pickupLocationID SEPARATOR ';') as pickupID\n" +
                    "FROM (car_info LEFT OUTER JOIN numbered_pickup ON car_info.id = numbered_pickup.carID)\n" +
                    "\t\t\tLEFT OUTER JOIN PickupLocation ON numbered_pickup.pickupLocationID = PickupLocation.id AND numbered_pickup.num < 4\n" +
                    "GROUP BY car_info.id\n" +
                    "ORDER BY rating DESC; ";

            // Perform the query
            ResultSet rs = statement.executeQuery(query);
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
                String location_phone = rs.getString("phoneNumber").replace(";", "<br>");
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
                // FOR DEBUGGING: System.out.println(jsonObject.toString());
                jsonArray.add(jsonObject);
            }
            
            // write JSON string to output
            out.write(jsonArray.toString());
            // set response status to 200 (OK)
            response.setStatus(200);

            rs.close();
            statement.close();
            dbcon.close();
        } catch (Exception e) {
        	
			// write error message JSON object to output
			JsonObject jsonObject = new JsonObject();
			jsonObject.addProperty("errorMessage", e.getMessage());
			out.write(jsonObject.toString());

			// set response status to 500 (Internal Server Error)
			response.setStatus(500);

        }
        out.close();

    }
}
