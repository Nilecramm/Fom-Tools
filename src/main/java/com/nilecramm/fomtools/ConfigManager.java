package com.nilecramm.fomtools;

import java.io.*;
import java.util.Properties;

/**
 * Manages application configuration stored in a properties file.
 * Handles loading and saving of user preferences such as folder paths and editor settings.
 */
public class ConfigManager {
    private static final String CONFIG_FILE = "options.properties";
    private static final String CHARACTERS_PATH_KEY = "characters.path";
    private static final String EDITOR_PATH_KEY = "editor.path";
    private static final String DEFAULT_PATH = "";
    
    private final Properties properties;

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
     * @return the characters folder path, or empty string if not set
     */
    public String getCharactersPath() {
        return properties.getProperty(CHARACTERS_PATH_KEY, DEFAULT_PATH);
    }

    /**
     * Sets the characters folder path
     * @param path the path to the characters folder
     */
    public void setCharactersPath(String path) {
        properties.setProperty(CHARACTERS_PATH_KEY, path);
        saveConfig();
    }

    /**
     * Gets the custom editor path
     * @return the custom editor path, or empty string if not set
     */
    public String getCustomEditorPath() {
        return properties.getProperty(EDITOR_PATH_KEY, DEFAULT_PATH);
    }

    /**
     * Sets the custom editor path
     * @param path the path to the custom editor
     */
    public void setCustomEditorPath(String path) {
        properties.setProperty(EDITOR_PATH_KEY, path);
        saveConfig();
    }
}