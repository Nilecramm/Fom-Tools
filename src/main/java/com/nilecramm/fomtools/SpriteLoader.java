package com.nilecramm.fomtools;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Classe utilitaire pour la recherche de sprites
 */
public class SpriteLoader {
    // Map de tous les sprites trouvés, organisés par partie du corps et frame
    private final Map<String, Map<Integer, String>> spritePaths = new HashMap<>();

    // Les clés de recherche pour chaque partie du corps - maintenant avec tous les éléments
    private static final List<String> BODY_PARTS = List.of(
            "base_arm_left", "base_arm_right", "base_chest", "base_head", "base_legs",
            "sleeve_left", "tool", "tool_effect", "sleeve_right", "head_gear",
            "face_gear", "hair_front", "eyes", "hair_mid", "face", "facial_hair",
            "waist", "feet", "legs", "torso", "back_gear", "hair_back",
            "head_gear_back", "base_effect"
    );

    /**
     * Charge tous les sprites d'un personnage spécifique
     * @param characterPath Chemin vers le dossier du personnage
     */
    public void loadCharacterSprites(String characterPath) {
        // Vider les chemins existants
        spritePaths.clear();

        // Initialiser les maps pour chaque partie du corps
        for (String part : BODY_PARTS) {
            spritePaths.put(part, new HashMap<>());
        }

        // Charger récursivement tous les fichiers du dossier
        File characterDir = new File(characterPath);
        scanDirectory(characterDir);
    }

    /**
     * Parcourt récursivement un dossier pour trouver tous les fichiers d'image
     * @param directory Le dossier à parcourir
     */
    private void scanDirectory(File directory) {
        if (!directory.isDirectory()) {
            return;
        }

        // Parcourir tous les fichiers du dossier
        File[] files = directory.listFiles();
        if (files == null) return;

        for (File file : files) {
            if (file.isDirectory()) {
                // Si c'est un dossier, explorer récursivement
                scanDirectory(file);
            } else if (isImageFile(file.getName())) {
                // Si c'est une image, essayer de la catégoriser
                categorizeImage(file);
            }
        }
    }

    /**
     * Vérifie si un fichier est une image
     * @param fileName Nom du fichier
     * @return true si c'est une image
     */
    private boolean isImageFile(String fileName) {
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".png") || lowerCase.endsWith(".jpg") ||
                lowerCase.endsWith(".jpeg") || lowerCase.endsWith(".gif");
    }

    /**
     * Catégorise une image selon sa partie du corps et son frame
     * @param file Le fichier image
     */
    private void categorizeImage(File file) {
        String fileName = file.getName();

        // Pour chaque partie du corps, vérifier si le nom contient la clé
        for (String bodyPart : BODY_PARTS) {
            if (fileName.contains(bodyPart)) {
                // Essayer de trouver un numéro de frame dans le nom
                Optional<Integer> frameNumber = extractFrameNumber(fileName);

                if (frameNumber.isPresent()) {
                    // Ajouter le chemin à la map
                    spritePaths.get(bodyPart).put(
                            frameNumber.get(),
                            "file:" + file.getAbsolutePath().replace("\\", "/")
                    );
                }

                // On suppose qu'une image correspond à une seule partie du corps
                break;
            }
        }
    }

    /**
     * Extrait le numéro de frame d'un nom de fichier
     * @param fileName Nom du fichier
     * @return Numéro de frame optionnel
     */
    private Optional<Integer> extractFrameNumber(String fileName) {
        // Supprime l'extension
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

        // Cherche tous les nombres dans le nom
        List<Integer> numbers = new ArrayList<>();
        StringBuilder currentNumber = new StringBuilder();

        for (char c : nameWithoutExt.toCharArray()) {
            if (Character.isDigit(c)) {
                currentNumber.append(c);
            } else if (currentNumber.length() > 0) {
                numbers.add(Integer.parseInt(currentNumber.toString()));
                currentNumber = new StringBuilder();
            }
        }

        // Ajouter le dernier nombre s'il existe
        if (currentNumber.length() > 0) {
            numbers.add(Integer.parseInt(currentNumber.toString()));
        }

        // Retourner le dernier nombre trouvé (généralement le frame)
        return numbers.isEmpty() ? Optional.empty() : Optional.of(numbers.get(numbers.size() - 1));
    }

    /**
     * Récupère le chemin d'une image spécifique
     * @param bodyPart Partie du corps
     * @param frameIndex Indice de frame
     * @return Chemin de l'image ou null si non trouvée
     */
    public String getSpritePath(String bodyPart, int frameIndex) {
        Map<Integer, String> frames = spritePaths.get(bodyPart);
        return frames != null ? frames.get(frameIndex) : null;
    }

    /**
     * Liste les dossiers de personnages disponibles
     * @param basePath Chemin de base des personnages
     * @return Liste des noms de dossiers
     */
    public static List<String> listCharacterFolders(String basePath) {
        File baseDir = new File(basePath);
        if (!baseDir.isDirectory()) {
            return Collections.emptyList();
        }

        File[] directories = baseDir.listFiles(File::isDirectory);
        if (directories == null) {
            return Collections.emptyList();
        }

        return Arrays.stream(directories)
                .map(File::getName)
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si les sprites de base nécessaires ont été trouvés
     * @return true si tous les sprites de base sont disponibles
     */
    public boolean hasBasicSprites() {
        // Vérifier uniquement les parties essentielles du corps
        List<String> essentialParts = List.of("base_arm_left", "base_arm_right", "base_chest", "base_head", "base_legs");

        for (String part : essentialParts) {
            if (spritePaths.get(part).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Vérifie si une partie spécifique a des sprites chargés
     * @param partName Nom de la partie
     * @return true si la partie a au moins un sprite
     */
    public boolean hasSpritesForPart(String partName) {
        Map<Integer, String> frames = spritePaths.get(partName);
        return frames != null && !frames.isEmpty();
    }

    /**
     * Obtient tous les frames disponibles pour une partie du corps
     * @param bodyPart Partie du corps
     * @return Set des indices de frame disponibles
     */
    public Set<Integer> getAvailableFrames(String bodyPart) {
        Map<Integer, String> frames = spritePaths.get(bodyPart);
        return frames != null ? frames.keySet() : Collections.emptySet();
    }

    /**
     * Liste toutes les parties du corps disponibles pour ce personnage
     * @return Liste des noms de parties qui ont au moins un sprite
     */
    public List<String> getAvailableBodyParts() {
        return BODY_PARTS.stream()
                .filter(this::hasSpritesForPart)
                .collect(Collectors.toList());
    }
}