package com.nilecramm.fomtools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ReadJson {
    
    public JsonData readJson(String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            JsonData jsonData = objectMapper.readValue(new File(fileName), JsonData.class);
            return jsonData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
