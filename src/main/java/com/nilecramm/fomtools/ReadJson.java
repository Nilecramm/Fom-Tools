package com.nilecramm.fomtools;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;

public class ReadJson {
    /*
    public static void main(String[] args) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Lire le fichier JSON et le mapper à l'objet Java
            JsonData jsonData = objectMapper.readValue(new File("src/data/animation/generated/par_output.json"), JsonData.class);

            //printFrames("base_arm_left", jsonData.base_arm_left);
            //printFrames("base_arm_right", jsonData.base_arm_right);
            //printFrames("base_chest", jsonData.base_chest);
            //printFrames("base_head", jsonData.base_head);
            //printFrames("base_legs", jsonData.base_legs);

            //on stocks chaques clés, on regarde un seul car de tt façon ils ont tous les mêmes clés
            ArrayList<String> keys = new ArrayList<String>(jsonData.base_arm_left.keySet());
            System.out.println("keys: " + keys);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

    public JsonData readJson(String fileName) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            // Lire le fichier JSON et le mapper à l'objet Java
            JsonData jsonData = objectMapper.readValue(new File(fileName), JsonData.class);
            return jsonData;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
    private static void printFrames(String armName, Map<String, ActionWrapper> armData) {
        System.out.println("data de: " + armName);
        for(Map.Entry<String, ActionWrapper> entry : armData.entrySet()) {
            String key = entry.getKey();
            ActionWrapper value = entry.getValue();
            System.out.println("key: " + key);
            System.out.println("value: " + value);

            // Print frames for each direction if they exist
            if (value.east != null) {
                System.out.println("Direction: east");
                for (Frame frame : value.east) {
                    System.out.println("  frame: " + frame);
                }
            }

            if (value.north != null) {
                System.out.println("Direction: north");
                for (Frame frame : value.north) {
                    System.out.println("  frame: " + frame);

                }
            }

            if (value.south != null) {
                System.out.println("Direction: south");
                for (Frame frame : value.south) {
                    System.out.println("  frame: " + frame);
                }
            }

            if (value.west != null) {
                System.out.println("Direction: west");
                for (Frame frame : value.west) {
                    System.out.println("  frame: " + frame);
                }
            }
        }
    }*/
}
