package com.nilecramm.fomtools;

import javafx.scene.image.Image;
import javafx.scene.image.PixelReader;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages Look-Up Tables (LUTs) for sprite color transformations.
 * LUTs allow applying color palettes to sprites for customization.
 */
public class LUTManager {
    private static final double COLOR_TOLERANCE = 0.01;
    private static final double MIN_COLOR_THRESHOLD = 0.01;
    
    private final Map<String, Image> loadedLUTs = new HashMap<>();
    private final Map<String, Integer> selectedColors = new HashMap<>();
    private final Map<String, Map<Color, Integer>> colorMappings = new HashMap<>();

    /**
     * Loads a LUT file and analyzes template colors
     * @param partName the body part name
     * @param lutPath the path to the LUT file
     * @return true if loading was successful
     */
    public boolean loadLUT(String partName, String lutPath) {
        try {
            File lutFile = new File(lutPath);
            if (!lutFile.exists()) {
                return false;
            }

            Image lutImage = new Image(lutFile.toURI().toString());
            loadedLUTs.put(partName, lutImage);
            selectedColors.put(partName, 0);

            // Analyser les couleurs template (ligne 0 de chaque colonne)
            analyzeLUTColors(partName, lutImage);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Gets the indices of available color variants
     * @param partName the body part name
     * @return list of available variant indices
     */
    public List<Integer> getAvailableVariantIndices(String partName) {
        Image lut = loadedLUTs.get(partName);
        Map<Color, Integer> colorMapping = colorMappings.get(partName);

        if (lut == null || colorMapping == null || colorMapping.isEmpty()) {
            return new ArrayList<>();
        }

        int width = (int) lut.getWidth();
        List<Integer> validIndices = new ArrayList<>();

        // Variants are COLUMNS, not rows
        for (int x = 0; x < width; x++) {
            validIndices.add(x);
        }

        System.out.println("Detected variants for " + partName + ": " + validIndices.size() + " columns");
        return validIndices;
    }

    /**
     * Applies the LUT to a sprite image
     * @param partName the body part name
     * @param originalSprite the original sprite image
     * @return the transformed sprite image with LUT applied, or original if no LUT is set
     */
    public Image applyLUT(String partName, Image originalSprite) {
        Image lut = loadedLUTs.get(partName);
        Integer variantColumn = selectedColors.get(partName);
        Map<Color, Integer> colorMapping = colorMappings.get(partName);

        if (lut == null || variantColumn == null || variantColumn == 0 || colorMapping == null) {
            return originalSprite; // No LUT or base color
        }

        int width = (int) originalSprite.getWidth();
        int height = (int) originalSprite.getHeight();

        WritableImage result = new WritableImage(width, height);
        PixelReader spriteReader = originalSprite.getPixelReader();
        PixelReader lutReader = lut.getPixelReader();
        PixelWriter resultWriter = result.getPixelWriter();

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Color originalColor = spriteReader.getColor(x, y);

                if (originalColor.getOpacity() > 0) {
                    Integer templateRow = findMatchingTemplateColor(originalColor, colorMapping);

                    if (templateRow != null) {
                        // Replace with the selected variant color (same row, different column)
                        Color newColor = lutReader.getColor(variantColumn, templateRow);
                        resultWriter.setColor(x, y, newColor);
                    } else {
                        resultWriter.setColor(x, y, originalColor);
                    }
                } else {
                    resultWriter.setColor(x, y, originalColor);
                }
            }
        }

        return result;
    }

    /**
     * Finds the LUT row corresponding to a sprite color
     */
    private Integer findMatchingTemplateColor(Color spriteColor, Map<Color, Integer> colorMapping) {
        for (Map.Entry<Color, Integer> entry : colorMapping.entrySet()) {
            if (colorsMatch(spriteColor, entry.getKey())) {
                return entry.getValue(); // Returns the ROW, not the column
            }
        }
        return null;
    }

    private void analyzeLUTColors(String partName, Image lutImage) {
        PixelReader reader = lutImage.getPixelReader();
        Map<Color, Integer> mapping = new HashMap<>();

        int height = (int) lutImage.getHeight();

        System.out.println("=== LUT Analysis for " + partName + " ===");
        System.out.println("Size: " + (int)lutImage.getWidth() + "x" + height);

        // Analyze ALL rows of column 0 (template colors)
        for (int y = 0; y < height; y++) {
            Color templateColor = reader.getColor(0, y); // Column 0 only

            if (templateColor.getOpacity() > 0 &&
                    (templateColor.getRed() > MIN_COLOR_THRESHOLD || 
                     templateColor.getGreen() > MIN_COLOR_THRESHOLD || 
                     templateColor.getBlue() > MIN_COLOR_THRESHOLD)) {

                mapping.put(templateColor, y); // Associate color -> ROW
                System.out.println("Template color detected at row " + y +
                        ": R=" + String.format("%.3f", templateColor.getRed()) +
                        ", G=" + String.format("%.3f", templateColor.getGreen()) +
                        ", B=" + String.format("%.3f", templateColor.getBlue()));
            }
        }

        colorMappings.put(partName, mapping);
        System.out.println("Total: " + mapping.size() + " template colors detected");
        System.out.println("===============================");
    }

    /**
     * Gets the number of available variants (for compatibility)
     * @param partName the body part name
     * @return the number of available color variants
     */
    public int getColorCount(String partName) {
        return getAvailableVariantIndices(partName).size();
    }

    /**
     * Compares two colors with a small tolerance for compression differences
     */
    private boolean colorsMatch(Color c1, Color c2) {
        return Math.abs(c1.getRed() - c2.getRed()) < COLOR_TOLERANCE &&
                Math.abs(c1.getGreen() - c2.getGreen()) < COLOR_TOLERANCE &&
                Math.abs(c1.getBlue() - c2.getBlue()) < COLOR_TOLERANCE;
    }

    /**
     * Sets the selected color variant for a body part
     * @param partName the body part name
     * @param colorIndex the color variant index
     */
    public void setSelectedColor(String partName, int colorIndex) {
        selectedColors.put(partName, colorIndex);
    }

    /**
     * Checks if a body part has a loaded LUT
     * @param partName the body part name
     * @return true if a LUT is loaded for this part
     */
    public boolean hasLUT(String partName) {
        return loadedLUTs.containsKey(partName);
    }

    /**
     * Removes the LUT for a body part
     * @param partName the body part name
     */
    public void removeLUT(String partName) {
        loadedLUTs.remove(partName);
        selectedColors.remove(partName);
    }
}