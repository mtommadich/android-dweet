package com.underdogfood.dweetproject;

import android.support.annotation.Nullable;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Scanner;

/**
 * DweetIO is a wrapper class around dweet.io web API.
 *
 * @author Khaled Bakhit <kb@khaled-bakhit.com>
 * Code refactoring By M. Tommadich
 * - ported java maven project to gradle for android project
 * - removed superfluous classes
 * - removed superfluous imports
 * - removed superfluous methods and method calls
 * - removed superfluous data members
 * - removed superfluous exception handlers
 */
public class DweetIO {

    /**
     * Instance of JsonParser to parse Json content .
     */

    private static final JsonParser jsonParser = new JsonParser();

    /**
     * Publish a dweet.
     * @param thingName Name of thing
     * @param content   Content of the dweet
     * @return True if successful
     * @throws IOException
     */
    public static boolean publish(String thingName, JsonElement content) throws IOException {

        thingName = URLEncoder.encode(thingName, "UTF-8");
        URL url = new URL("http://dweet.io/dweet/for/" + thingName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
        connection.setRequestMethod("POST");
        connection.setDoInput(true);
        connection.setDoOutput(true);

        PrintWriter out = new PrintWriter(connection.getOutputStream());
        out.println(content.toString());
        out.flush();
        out.close();

        JsonObject response = readResponse(connection.getInputStream());
        connection.disconnect();

        return (response.has("this") && response.get("this").getAsString().equals("succeeded"));
    }


    /**
     * Get the latest dweet for a thing.
     *
     * @param thingName Name of the thing to get the latest dweet from.
     * @return Latest dweet or null if not available.
     * @throws IOException              Unable to complete the API call.
     * @throws java.text.ParseException Unable to parse creation date in dweets.
     */

    @Nullable
    public static Dweet getLatestDweet(String thingName) throws IOException {
        if (thingName == null) {
            throw new NullPointerException();
        }

        thingName = URLEncoder.encode(thingName, "UTF-8");
        URL url = new URL("http://dweet.io/get/latest/dweet/for/" + thingName);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        JsonObject response = readResponse(connection.getInputStream());

        connection.disconnect();

        if (response.has("this") && response.get("this").getAsString().equals("succeeded")) {
            JsonArray arr = response.getAsJsonArray("with");
            if (arr.size() == 0)
                return null;
            return new Dweet(arr.remove(0));
        }
        return null;
    }

    private static JsonObject readResponse(InputStream in) {
        Scanner scanner = new Scanner(in);
        StringBuilder string = new StringBuilder();
        while (scanner.hasNext())
            string.append(scanner.nextLine()).append('\n');
        scanner.close();
        return jsonParser.parse(string.toString()).getAsJsonObject();
    }
}
