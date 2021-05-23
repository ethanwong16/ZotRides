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
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Declaring a WebServlet called SearchServlet, which maps to url "/api/full-text-search"
@WebServlet(name = "FullTextSearchServlet", urlPatterns = "/api/full-text-search")
public class FullTextSearchServlet extends HttpServlet {
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
         *   - Output : same as regular search */

        // Retrieve parameters from url request
        String token = request.getParameter("token");

        PrintWriter out = response.getWriter();
        HttpSession session = request.getSession();
        CarListSettings previousSettings = (CarListSettings) session.getAttribute("previousSettings");

        String additional = "";
        if (token == null || token.trim().isEmpty()) {
            // return empty results
            JsonObject result = new JsonObject();
            result.add("results", new JsonArray());
            result.addProperty("message", "No results. Page 1");
            JsonObject sorting = new JsonObject();
            sorting.addProperty("ratingFirst", 0);
            sorting.addProperty("ratingDescend", 0);
            sorting.addProperty("nameDescend", 0);
            result.add("sorting", sorting);
            out.write(result.toString());
            out.close();

            if (previousSettings != null) {          // no valid search yet
                synchronized (previousSettings) {
                    previousSettings.setCache(null); // indicates empty results
                }
            }
            return;
        }

        String[] tokens = token.trim().split("\\s+");
        for (String curr : tokens) {
            additional += "+" + curr + "*";
        }

        System.out.println("tokens: " + additional);

        int fuzzyThreshold = token.trim().length() / 3;

        // query with fuzzy search
        String query = "WITH pickupCarCounts AS (SELECT pickupLocationID, COUNT(DISTINCT carID) as numCars \n" +
                "\tFROM pickup_car_from \n" +
                "    GROUP BY pickupLocationID),\n" +
                "    \n" +
                "    car_info AS (SELECT Cars.id as id, model, make, year, name, price, rating, numVotes\n" +
                "\tFROM (((category_of_car, Category, Cars) LEFT OUTER JOIN CarPrices ON Cars.id = CarPrices.carID)\n" +
                "\t\t\tLEFT OUTER JOIN Ratings ON Ratings.carID = Cars.id)\n" +
                "\tWHERE category_of_car.categoryID = Category.id\n" +
                "\t\t\tAND Cars.id = category_of_car.carID \n" +
                "\t\t\tAND (MATCH(model) AGAINST (? IN BOOLEAN MODE) OR edth(model, ?, "+ fuzzyThreshold + ")))\n" + //TODO: added edth for fuzzy
                "    \n" +
                "SELECT car_info.id as id, group_concat(DISTINCT concat_ws(' ', make, model, year)) as name, \n" +
                "\t\tname as category, price, rating, numVotes,\n" +
                "        group_concat(DISTINCT address ORDER BY numCars DESC, address SEPARATOR ';') as address, \n" +
                "        group_concat(DISTINCT phoneNumber ORDER BY numCars DESC, address SEPARATOR ';') as phoneNumber,\n" +
                "        group_concat(DISTINCT PickupLocation.id ORDER BY numCars DESC, address SEPARATOR ';') as pickupID\n" +
                "FROM ((car_info LEFT OUTER JOIN pickup_car_from ON pickup_car_from.carID = car_info.id) \n" +
                "\tLEFT OUTER JOIN pickupCarCounts ON pickup_car_from.pickupLocationID = pickupCarCounts.pickupLocationID)\n" +
                "\tLEFT OUTER JOIN PickupLocation ON pickup_car_from.pickupLocationID = PickupLocation.id\n" +
                "GROUP BY car_info.id\n";

        /* // query without fuzzy search
        String query = "WITH pickupCarCounts AS (SELECT pickupLocationID, COUNT(DISTINCT carID) as numCars \n" +
                "\tFROM pickup_car_from \n" +
                "    GROUP BY pickupLocationID),\n" +
                "    \n" +
                "    car_info AS (SELECT Cars.id as id, model, make, year, name, price, rating, numVotes\n" +
                "\tFROM (((category_of_car, Category, Cars) LEFT OUTER JOIN CarPrices ON Cars.id = CarPrices.carID)\n" +
                "\t\t\tLEFT OUTER JOIN Ratings ON Ratings.carID = Cars.id)\n" +
                "\tWHERE category_of_car.categoryID = Category.id\n" +
                "\t\t\tAND Cars.id = category_of_car.carID \n" +
                "\t\t\tAND MATCH(model) AGAINST (? IN BOOLEAN MODE))\n" +
                "    \n" +
                "SELECT car_info.id as id, group_concat(DISTINCT concat_ws(' ', make, model, year)) as name, \n" +
                "\t\tname as category, price, rating, numVotes,\n" +
                "        group_concat(DISTINCT address ORDER BY numCars DESC, address SEPARATOR ';') as address, \n" +
                "        group_concat(DISTINCT phoneNumber ORDER BY numCars DESC, address SEPARATOR ';') as phoneNumber,\n" +
                "        group_concat(DISTINCT PickupLocation.id ORDER BY numCars DESC, address SEPARATOR ';') as pickupID\n" +
                "FROM ((car_info LEFT OUTER JOIN pickup_car_from ON pickup_car_from.carID = car_info.id) \n" +
                "\tLEFT OUTER JOIN pickupCarCounts ON pickup_car_from.pickupLocationID = pickupCarCounts.pickupLocationID)\n" +
                "\tLEFT OUTER JOIN PickupLocation ON pickup_car_from.pickupLocationID = PickupLocation.id\n" +
                "GROUP BY car_info.id\n"; */

//        System.out.println("query: \n" + query);

        // Store base query into session (to maintain consistency when jumping back to CarsList page)
        if (previousSettings == null) {
            previousSettings = new CarListSettings(query, 1, 20);
            session.setAttribute("previousSettings", previousSettings);
        } else {
            // prevent corrupted states through sharing under multi-threads
            // will only be executed by one thread at a time
            synchronized (previousSettings) {
                previousSettings.reset(query, 1, 20);
            }
        }

        // Run query & return response
        response.setContentType("application/json"); // Response mime type
        try (Connection conn = dataSource.getConnection()) {
            // run query
            query = previousSettings.toQuery();
            PreparedStatement statement = conn.prepareStatement(query);
            statement.setString(1, additional);
            statement.setString(2, token.trim()); //TODO: ADDED THIS

//            System.out.println("query:\n" + statement.toString() + "\n");
            ResultSet rs = statement.executeQuery();

            // process results
            JsonArray jsonArray = new JsonArray();
            int count = 0;
            //Pattern firstThree = Pattern.compile("^([^;]+;[^;]+;[^;]+).*");
            Pattern firstThree = Pattern.compile("^(([^;]+;{0,1}){0,3}).*");
            while (rs.next() && count++ < 100) { // Iterate through each row of rs
                String car_id = rs.getString("id");
                String car_name = rs.getString("name");
                String car_category = rs.getString("category");
                double car_rating = rs.getDouble("rating");
                int car_votes = rs.getInt("numVotes");
                String location_address = rs.getString("address");
                String location_phone = rs.getString("phoneNumber");
                String location_ids = rs.getString("pickupID");

                // Create a JsonObject based on the data we retrieve from rs
                JsonObject jsonObject = new JsonObject();
                jsonObject.addProperty("car_id", car_id);
                jsonObject.addProperty("car_name", car_name);
                jsonObject.addProperty("car_category", car_category);
                jsonObject.addProperty("car_rating", car_rating);
                jsonObject.addProperty("car_votes", car_votes);

                // Filter out only the top 3 based on sorted order
                if (location_address == null || location_phone == null || location_ids == null) {
                    jsonObject.addProperty("location_address", "");
                    jsonObject.addProperty("location_phone", "");
                    jsonObject.addProperty("location_ids", "");
                } else {
                    Matcher addresses = firstThree.matcher(location_address);
                    Matcher phones = firstThree.matcher(location_phone);
                    Matcher ids = firstThree.matcher(location_ids);
                    addresses.find();
                    phones.find();
                    ids.find();

                    String addr = addresses.group(1);
                    String phone = phones.group(1);
                    String ID = ids.group(1);

//                System.out.println("addr: " + addr);
//                System.out.println("phone: " + phone);
//                System.out.println("ID: " + ID);

                    if (addr != null && addr.length() != 0)
                        jsonObject.addProperty("location_address", addr.charAt(addr.length() - 1) == ';' ? addr.substring(0, addr.length() - 1) : addr);
                    else
                        jsonObject.addProperty("location_address", "");

                    if (phone != null && phone.length() != 0)
                        jsonObject.addProperty("location_phone", phone.charAt(phone.length() - 1) == ';' ? phone.substring(0, phone.length() - 1) : phone);
                    else
                        jsonObject.addProperty("location_phone", "");

                    if (ID != null && ID.length() != 0)
                        jsonObject.addProperty("location_ids", ID.charAt(ID.length() - 1) == ';' ? ID.substring(0, ID.length() - 1) : ID);
                    else
                        jsonObject.addProperty("location_ids", "");
                }

                jsonArray.add(jsonObject);
            }

            // cache at most 100 returned results
            synchronized (previousSettings) {
                previousSettings.setCache(jsonArray);
            }

//            System.out.println(jsonArray);

            // return results
            JsonArray firstTwenty = new JsonArray();
            for (int i = 0; i < 20 && i < jsonArray.size(); ++i) {
                firstTwenty.add(jsonArray.get(i));
            }

            JsonObject result = new JsonObject();
            result.add("results", firstTwenty);
            result.addProperty("message", previousSettings.getPaginationMessage());
            out.write(result.toString());
            response.setStatus(200);         // set response status to 200 (OK)
            rs.close();
            statement.close();

            // get the max number of results
            String len =  "SELECT COUNT(DISTINCT Cars.id) as numResults \n" +
                    "\tFROM category_of_car, Category, Cars\n" +
                    "\tWHERE category_of_car.categoryID = Category.id\n" +
                    "\t\t\tAND Cars.id = category_of_car.carID \n" +
                    "\t\t\tAND MATCH(model) AGAINST (? IN BOOLEAN MODE);";

            statement = conn.prepareStatement(len);
            statement.setString(1, additional);
//            System.out.println("-----------------\n" + statement.toString());
            rs = statement.executeQuery(); // Perform the query
            if (rs.next()) {
                int maxResults = rs.getInt("numResults");
                synchronized (previousSettings) {
                    previousSettings.setMaxNumResults(maxResults);
                }
                System.out.println("stored max results: " + maxResults);
            }
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