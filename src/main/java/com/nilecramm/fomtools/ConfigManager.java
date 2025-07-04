package com.nilecramm.fomtools;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    private static final String CONFIG_FILE = "options.properties";
    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        loadConfig();
    }

    /**
     * Loads configuration from file
     */
    private void loadConfig() {
        File configFile = new File(CONFIG_FILE);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                properties.load(fis);
            } catch (IOException e) {
                System.err.println("Error loading configuration: " + e.getMessage());
            }
        }
    }

    /**
     * Saves configuration to file
     */
    private void saveConfig() {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            properties.store(fos, "Animation Viewer Configuration");
        } catch (IOException e) {
            System.err.println("Error saving configuration: " + e.getMessage());
        }
    }

    /**
     * Gets the characters folder path
     */
    public String getCharactersPath() {
        return properties.getProperty("characters.path", "");
    }

    /**
     * Sets the characters folder path
     */
    public void setCharactersPath(String path) {
        properties.setProperty("characters.path", path);
        saveConfig();
    }

    /**
     * Gets the custom editor path
     */
    public String getCustomEditorPath() {
        return properties.getProperty("editor.path", "");
    }

    /**
     * Sets the custom editor path
     */
    public void setCustomEditorPath(String path) {
        properties.setProperty("editor.path", path);
        saveConfig();
    }
}