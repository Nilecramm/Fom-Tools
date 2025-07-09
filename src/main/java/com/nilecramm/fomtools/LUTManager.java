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

public class LUTManager {
    private final Map<String, Image> loadedLUTs = new HashMap<>();
    private final Map<String, Integer> selectedColors = new HashMap<>();
    private final Map<String, Map<Color, Integer>> colorMappings = new HashMap<>();

    /**
     * Charge un fichier LUT et analyse les couleurs template
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
     * Obtient les indices des variantes disponibles
     */
    public List<Integer> getAvailableVariantIndices(String partName) {
        Image lut = loadedLUTs.get(partName);
        Map<Color, Integer> colorMapping = colorMappings.get(partName);

        if (lut == null || colorMapping == null || colorMapping.isEmpty()) {
            return new ArrayList<>();
        }

        int width = (int) lut.getWidth();
        List<Integer> validIndices = new ArrayList<>();

        // Les variantes sont les COLONNES, pas les lignes !
        for (int x = 0; x < width; x++) {
            validIndices.add(x);
        }

        System.out.println("Variantes détectées pour " + partName + ": " + validIndices.size() + " colonnes");
        return validIndices;
    }

    /**
     * Applique la LUT à un sprite
     */
    public Image applyLUT(String partName, Image originalSprite) {
        Image lut = loadedLUTs.get(partName);
        Integer variantColumn = selectedColors.get(partName);
        Map<Color, Integer> colorMapping = colorMappings.get(partName);

        if (lut == null || variantColumn == null || variantColumn == 0 || colorMapping == null) {
            return originalSprite; // Pas de LUT ou couleur de base
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
                    // Chercher si cette couleur correspond à une couleur template (colonne 0)
                    Integer templateRow = findMatchingTemplateColor(originalColor, colorMapping);

                    if (templateRow != null) {
                        // Remplacer par la couleur de la variante sélectionnée (même ligne, colonne différente)
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
     * Trouve la ligne LUT correspondant à une couleur du sprite
     */
    private Integer findMatchingTemplateColor(Color spriteColor, Map<Color, Integer> colorMapping) {
        for (Map.Entry<Color, Integer> entry : colorMapping.entrySet()) {
            if (colorsMatch(spriteColor, entry.getKey())) {
                return entry.getValue(); // Retourne la LIGNE, pas la colonne
            }
        }
        return null;
    }

    private void analyzeLUTColors(String partName, Image lutImage) {
        PixelReader reader = lutImage.getPixelReader();
        Map<Color, Integer> mapping = new HashMap<>();

        int height = (int) lutImage.getHeight();

        System.out.println("=== Analyse LUT pour " + partName + " ===");
        System.out.println("Taille: " + (int)lutImage.getWidth() + "x" + height);

        // Analyser TOUTES les lignes de la colonne 0 (couleurs template)
        for (int y = 0; y < height; y++) {
            Color templateColor = reader.getColor(0, y); // COLONNE 0 seulement

            if (templateColor.getOpacity() > 0 &&
                    (templateColor.getRed() > 0.01 || templateColor.getGreen() > 0.01 || templateColor.getBlue() > 0.01)) {

                mapping.put(templateColor, y); // Associer couleur -> LIGNE
                System.out.println("Couleur template détectée à la ligne " + y +
                        ": R=" + String.format("%.3f", templateColor.getRed()) +
                        ", G=" + String.format("%.3f", templateColor.getGreen()) +
                        ", B=" + String.format("%.3f", templateColor.getBlue()));
            }
        }

        colorMappings.put(partName, mapping);
        System.out.println("Total: " + mapping.size() + " couleurs template détectées");
        System.out.println("===============================");
    }

    /**
     * Obtient le nombre de variantes disponibles (pour compatibilité)
     */
    public int getColorCount(String partName) {
        return getAvailableVariantIndices(partName).size();
    }

    /**
     * Compare deux couleurs avec une petite tolerance pour les différences de compression
     */
    private boolean colorsMatch(Color c1, Color c2) {
        double tolerance = 0.01; // Tolérance pour les différences de compression PNG

        return Math.abs(c1.getRed() - c2.getRed()) < tolerance &&
                Math.abs(c1.getGreen() - c2.getGreen()) < tolerance &&
                Math.abs(c1.getBlue() - c2.getBlue()) < tolerance;
    }

    /**
     * Définit la couleur sélectionnée pour une partie
     */
    public void setSelectedColor(String partName, int colorIndex) {
        selectedColors.put(partName, colorIndex);
    }

    /**
     * Vérifie si une partie a une LUT chargée
     */
    public boolean hasLUT(String partName) {
        return loadedLUTs.containsKey(partName);
    }

    /**
     * Supprime la LUT d'une partie
     */
    public void removeLUT(String partName) {
        loadedLUTs.remove(partName);
        selectedColors.remove(partName);
    }
}