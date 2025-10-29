package com.nilecramm.fomtools;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility class for finding and loading sprite images.
 * Scans directories recursively to locate sprite files and categorize them by body part.
 */
public class SpriteLoader {
    // Map of all found sprites, organized by body part and frame index
    private final Map<String, Map<Integer, String>> spritePaths = new HashMap<>();

    // Search keys for each body part - complete list
    private static final List<String> BODY_PARTS = List.of(
            "base_arm_left", "base_arm_right", "base_chest", "base_head", "base_legs",
            "sleeve_left", "tool", "tool_effect", "sleeve_right", "head_gear",
            "face_gear", "hair_front", "eyes", "hair_mid", "face", "facial_hair",
            "waist", "feet", "legs", "torso", "back_gear", "hair_back",
            "head_gear_back", "base_effect"
    );

    /**
     * Loads all sprites for a specific character
     * @param characterPath path to the character folder
     */
    public void loadCharacterSprites(String characterPath) {
        // Clear existing paths
        spritePaths.clear();

        // Initialize maps for each body part
        for (String part : BODY_PARTS) {
            spritePaths.put(part, new HashMap<>());
        }

        // Recursively load all files from the folder
        File characterDir = new File(characterPath);
        scanDirectory(characterDir);
    }

    /**
     * Recursively scans a directory to find all image files
     * @param directory the directory to scan
     */
    private void scanDirectory(File directory) {
        if (!directory.isDirectory()) {
            return;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return;
        }

        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectory(file);
            } else if (isImageFile(file.getName())) {
                categorizeImage(file);
            }
        }
    }

    /**
     * Checks if a file is an image based on its extension
     * @param fileName the file name
     * @return true if the file is an image
     */
    private boolean isImageFile(String fileName) {
        String lowerCase = fileName.toLowerCase();
        return lowerCase.endsWith(".png") || lowerCase.endsWith(".jpg") ||
                lowerCase.endsWith(".jpeg") || lowerCase.endsWith(".gif");
    }

    /**
     * Categorizes an image according to its body part and frame number
     * @param file the image file
     */
    private void categorizeImage(File file) {
        String fileName = file.getName();

        // Check if the filename contains any body part identifier
        for (String bodyPart : BODY_PARTS) {
            if (fileName.contains(bodyPart)) {
                Optional<Integer> frameNumber = extractFrameNumber(fileName);

                if (frameNumber.isPresent()) {
                    spritePaths.get(bodyPart).put(
                            frameNumber.get(),
                            "file:" + file.getAbsolutePath().replace("\\", "/")
                    );
                }

                // Assume one image corresponds to one body part
                break;
            }
        }
    }

    /**
     * Extracts the frame number from a filename
     * @param fileName the file name
     * @return optional frame number
     */
    private Optional<Integer> extractFrameNumber(String fileName) {
        // Remove extension
        String nameWithoutExt = fileName.substring(0, fileName.lastIndexOf('.'));

        // Find all numbers in the name
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

        // Add the last number if it exists
        if (currentNumber.length() > 0) {
            numbers.add(Integer.parseInt(currentNumber.toString()));
        }

        // Return the last number found (typically the frame number)
        return numbers.isEmpty() ? Optional.empty() : Optional.of(numbers.get(numbers.size() - 1));
    }

    /**
     * Gets the path to a specific sprite image
     * @param bodyPart the body part
     * @param frameIndex the frame index
     * @return the image path or null if not found
     */
    public String getSpritePath(String bodyPart, int frameIndex) {
        Map<Integer, String> frames = spritePaths.get(bodyPart);
        return frames != null ? frames.get(frameIndex) : null;
    }

    /**
     * Lists available character folders
     * @param basePath the base path to the characters directory
     * @return list of folder names
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
     * Checks if essential base sprites have been found
     * @return true if all essential base sprites are available
     */
    public boolean hasBasicSprites() {
        // Check only essential body parts
        List<String> essentialParts = List.of("base_arm_left", "base_arm_right", "base_chest", "base_head", "base_legs");

        for (String part : essentialParts) {
            if (spritePaths.get(part).isEmpty()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if a specific part has loaded sprites
     * @param partName the part name
     * @return true if the part has at least one sprite
     */
    public boolean hasSpritesForPart(String partName) {
        Map<Integer, String> frames = spritePaths.get(partName);
        return frames != null && !frames.isEmpty();
    }

    /**
     * Gets all available frames for a body part
     * @param bodyPart the body part
     * @return set of available frame indices
     */
    public Set<Integer> getAvailableFrames(String bodyPart) {
        Map<Integer, String> frames = spritePaths.get(bodyPart);
        return frames != null ? frames.keySet() : Collections.emptySet();
    }

    /**
     * Lists all available body parts for this character
     * @return list of part names that have at least one sprite
     */
    public List<String> getAvailableBodyParts() {
        return BODY_PARTS.stream()
                .filter(this::hasSpritesForPart)
                .collect(Collectors.toList());
    }
}