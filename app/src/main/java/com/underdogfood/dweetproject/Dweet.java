package com.underdogfood.dweetproject;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * @author Mario Tommadich
 */
public class Dweet{
    //data members
    private final JsonObject content;
    
    //constructor
    Dweet(JsonElement jDweet){
        JsonObject j = jDweet.getAsJsonObject();
        content = j.get("content").getAsJsonObject();
    }

    // Get the dweet contents.
    public JsonObject getContent(){
        return content;
    }
}
