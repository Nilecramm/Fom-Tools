package com.nilecramm.fomtools;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RenderAnimation {
    // Add at the class level in RenderAnimation
    private final Map<String, Integer> currentFrameMap = new HashMap<>();
    // Add this interface near the top of the file, after the class declaration
    public interface FrameUpdateListener {
        void onFrameUpdate(String partName, int frameIndex);
    }

    // Add this field to store the listener
    private FrameUpdateListener frameUpdateListener;

    // Add this method to set the listener
    public void setFrameUpdateListener(FrameUpdateListener listener) {
        this.frameUpdateListener = listener;
    }
    private boolean isPaused = false;
    private double scale = 1.0;
    private double speed = 1.0;

    // Loader pour les sprites
    private final SpriteLoader spriteLoader = new SpriteLoader();

    // Reference to the JsonData containing all animations
    private final JsonData jsonData;

    // Container for all sprite parts
    private final Pane spriteContainer;

    // Current animation state
    private String currentAction = "idle"; // Default action
    private String currentDirection = "south"; // Default direction

    // ImageViews for each body part
    private final Map<String, ImageView> bodyParts = new HashMap<>();

    // Current animation timelines for each body part
    private final Map<String, Timeline> timelines = new HashMap<>();

    // Liste complète des parties du corps
    private final List<String> allPartNames = List.of(
            "base_arm_left", "base_arm_right", "base_chest", "base_head", "base_legs",
            "sleeve_left", "tool", "tool_effect", "sleeve_right", "head_gear",
            "face_gear", "hair_front", "eyes", "hair_mid", "face", "facial_hair",
            "waist", "feet", "legs", "torso", "back_gear", "hair_back",
            "head_gear_back", "base_effect"
    );

    // Rendering order based on direction
    private final Map<String, List<String>> renderOrder = Map.of(
            "south", List.of(
                    "held_item", "sleeve_left", "base_arm_left", "tool", "tool_effect", "sleeve_right", "base_arm_right",
                    "head_gear", "face_gear", "hair_front", "eyes", "hair_mid", "face", "facial_hair", "base_head",
                    "waist", "feet", "legs", "base_legs", "torso", "base_chest", "back_gear", "hair_back",
                    "head_gear_back", "base_effect"
            ),
            "east", List.of(
                    "sleeve_left", "base_arm_left", "held_item", "tool", "tool_effect", "head_gear", "face_gear",
                    "hair_front", "eyes", "hair_mid", "face", "facial_hair", "base_head", "waist", "feet", "legs",
                    "base_legs", "torso", "base_chest", "sleeve_right", "base_arm_right", "hair_back", "back_gear",
                    "head_gear_back", "base_effect"
            ),
            "north", List.of(
                    "head_gear", "hair_back", "back_gear", "sleeve_left", "base_arm_left", "sleeve_right", "base_arm_right",
                    "hair_front", "hair_mid", "face_gear", "eyes", "face", "facial_hair", "base_head", "head_gear_back",
                    "waist", "feet", "legs", "base_legs", "torso", "base_chest", "held_item", "tool", "tool_effect",
                    "base_effect"
            )
    );


    /**
     * Create a new animation renderer
     * @param jsonData The animation data
     * @param container The pane to render the animation in
     */
    public RenderAnimation(JsonData jsonData, Pane container) {
        this.jsonData = jsonData;
        this.spriteContainer = container;

        // Initialize all body parts
        for (String part : allPartNames) {
            ImageView imageView = new ImageView();
            imageView.setPreserveRatio(true);
            bodyParts.put(part, imageView);
            spriteContainer.getChildren().add(imageView);
        }
    }

    public void togglePause() {
        isPaused = !isPaused;

        for (Timeline timeline : timelines.values()) {
            if (timeline != null) {
                if (isPaused) {
                    timeline.pause();
                } else {
                    timeline.play();
                }
            }
        }
    }

    // Add this getter
    public boolean isPaused() {
        return isPaused;
    }

    public SpriteLoader getSpriteLoader() {
        return this.spriteLoader;
    }

    /**
     * Charge les sprites d'un personnage spécifique
     * @param characterPath Chemin vers le dossier du personnage
     * @return true si le chargement a réussi
     */
    public boolean loadCharacter(String characterPath) {
        spriteLoader.loadCharacterSprites(characterPath);

        // Vérifier que les sprites de base nécessaires ont été trouvés
        boolean hasBasicSprites = spriteLoader.hasBasicSprites();

        // If loading was successful and we have an action, restart the animation
        if (hasBasicSprites && currentAction != null && !currentAction.isEmpty()) {
            // Store pause state
            boolean wasPaused = isPaused;

            // Ensure we're not paused before setting animation
            isPaused = false;

            // Set the animation
            setAnimation(currentAction, currentDirection);

            // Restore pause state explicitly if needed
            if (wasPaused) {
                isPaused = true;
                for (Timeline timeline : timelines.values()) {
                    if (timeline != null) {
                        timeline.pause();
                    }
                }
            }
        }

        return hasBasicSprites;
    }

    /**
     * Force render the current frame (useful when paused)
     */
    public void renderCurrentFrame() {
        // Only process for parts that have a current timeline
        for (String partName : timelines.keySet()) {
            String imagePath = spriteLoader.getSpritePath(partName,
                    currentFrameMap.getOrDefault(partName, 1));
            if (imagePath != null && bodyParts.containsKey(partName)) {
                ImageView view = bodyParts.get(partName);
                view.setImage(new Image(imagePath));
            }
        }
    }

    /**
     * Set the animation playback speed
     * @param speed The speed factor (1.0 = normal speed)
     */
    public void setSpeed(double speed) {
        this.speed = speed;

        // Restart animations with new speed
        if (currentAction != null && !currentAction.isEmpty()) {
            setAnimation(currentAction, currentDirection);
        }
    }

    /**
     * Change the current animation
     * @param action The action to perform (e.g., "axe", "water", "idle")
     * @param direction The direction to face ("north", "east", "south")
     */
    public void setAnimation(String action, String direction) {
        // Stop all current animations
        stopAllAnimations();

        this.currentAction = action;
        this.currentDirection = direction;

        // Start the new animation for all body parts
        startAnimations();

        // If the animation was paused, make sure it stays paused
        if (isPaused) {
            for (Timeline timeline : timelines.values()) {
                if (timeline != null) {
                    timeline.pause();
                }
            }
        }
    }

    /**
     * Start animation for all body parts with current action and direction
     */
    private void startAnimations() {
        // Obtenir la liste des parties disponibles pour ce personnage
        List<String> availableParts = spriteLoader.getAvailableBodyParts();

        // Démarrer l'animation pour chaque partie disponible
        for (String partName : availableParts) {
            startAnimation(partName);
        }

        // Apply proper z-ordering based on current direction
        applyRenderOrder();
    }

    /**
     * Apply the proper render order based on current direction
     */
    private void applyRenderOrder() {
        List<String> order = renderOrder.getOrDefault(currentDirection, renderOrder.get("south"));

        // Reset all view orders first
        for (ImageView view : bodyParts.values()) {
            view.setViewOrder(0);
        }

        // Apply render order - IMPORTANT: in JavaFX, LOWER viewOrder appears IN FRONT
        for (int i = 0; i < order.size(); i++) {
            String part = order.get(i);
            if (bodyParts.containsKey(part)) {
                // First items in our list should get HIGHER values (to appear behind)
                // Last items in our list should get LOWER values (to appear in front)
                bodyParts.get(part).setViewOrder(i);
            }
        }
    }

    /**
     * Start animation for a specific body part
     * @param partName The body part to animate
     */
    @SuppressWarnings("unchecked")
    private void startAnimation(String partName) {
        Map<String, ActionWrapper> partActions;

        try {
            // Utiliser la réflexion pour accéder au champ correspondant dans jsonData
            Field field = jsonData.getClass().getField(partName);
            partActions = (Map<String, ActionWrapper>) field.get(jsonData);

            if (partActions == null) {
                System.out.println("Aucune action trouvée pour la partie " + partName);
                return;
            }
        } catch (Exception e) {
            System.out.println("Erreur d'accès à la partie " + partName + ": " + e.getMessage());
            return;
        }

        // Check if action exists
        if (!partActions.containsKey(currentAction)) {
            System.out.println("Action " + currentAction + " not found for " + partName);
            return;
        }

        ActionWrapper action = partActions.get(currentAction);
        Frame[] frames;

        // Get frames for current direction
        switch (currentDirection) {
            case "north": frames = action.north; break;
            case "east": frames = action.east; break;
            case "south": frames = action.south; break;
            default: frames = action.south; // Default to south
        }

        if (frames == null || frames.length == 0) {
            System.out.println("No frames found for " + partName + " in direction " + currentDirection);
            return;
        }

        // Create timeline for animation
        Timeline timeline = new Timeline();
        ImageView view = bodyParts.get(partName);

        // Vérifier que l'ImageView existe
        if (view == null) {
            System.out.println("ImageView manquant pour " + partName);
            return;
        }

        double totalDuration = 0;
        for (Frame frame : frames) {
            final int frameIndex = frame.target_frame;
            final int[] offset = frame.offset.clone(); // Clone to avoid modifying the original

            // Scale the offset based on current scale
            offset[0] = (int)(offset[0] * scale);
            offset[1] = (int)(offset[1] * scale);

            double adjustedDuration = frame.duration / speed;

            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(totalDuration),
                    e -> {
                        // In the KeyFrame handler inside startAnimation method, add this line:
                        currentFrameMap.put(partName, frameIndex);

                        // Utiliser le SpriteLoader pour trouver l'image
                        String imagePath = spriteLoader.getSpritePath(partName, frameIndex);

                        if (imagePath != null) {
                            // Charger l'image directement à la bonne taille selon le scale actuel
                            int originalWidth, originalHeight;
                            if (partName.equals("base_head") || partName.equals("eyes") || partName.equals("face") || partName.equals("facial_hair")) {
                                originalWidth = originalHeight = 16;
                            } else if (partName.equals("hair_back") || partName.equals("hair_mid")) {
                                originalWidth = originalHeight = 40;
                            } else {
                                originalWidth = originalHeight = 32;
                            }

                            int scaledWidth = (int) (originalWidth * scale);
                            int scaledHeight = (int) (originalHeight * scale);

                            // Charger l'image avec la taille voulue et sans lissage
                            Image image = new Image(imagePath, scaledWidth, scaledHeight, false, false);
                            view.setImage(image);

                            // Apply offset
                            view.setTranslateX(offset[0]);
                            view.setTranslateY(offset[1]);

                            // Original depth is still used for animations that
                            // don't follow the standard render order
                            if (frame.depth > 0) {
                                //view.setViewOrder(-frame.depth);
                            }
                            // Notify the listener about the frame update
                            if (frameUpdateListener != null) {
                                frameUpdateListener.onFrameUpdate(partName, frameIndex);
                            }
                        } else {
                            // Si l'image n'est pas trouvée, effacer l'image précédente
                            view.setImage(null);
                        }
                    }
            );

            timeline.getKeyFrames().add(keyFrame);
            totalDuration += adjustedDuration;
        }

        // Add a final keyframe to loop the animation
        KeyFrame loopFrame = new KeyFrame(Duration.seconds(totalDuration));
        timeline.getKeyFrames().add(loopFrame);

        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        // Store the timeline for later stopping
        timelines.put(partName, timeline);
    }

    /**
     * Stop all running animations
     */
    private void stopAllAnimations() {
        for (Timeline timeline : timelines.values()) {
            if (timeline != null) {
                timeline.stop();
            }
        }
        timelines.clear();

        // Réinitialiser toutes les images
        for (ImageView view : bodyParts.values()) {
            view.setImage(null);
        }
    }

    /**
     * Set the scale of the animation
     * @param scale The scale factor
     */
    public void setScale(double scale) {
        this.scale = scale;

        for (String partName : bodyParts.keySet()) {
            ImageView view = bodyParts.get(partName);

            // Déterminer la taille originale selon le type de partie
            int originalWidth, originalHeight;
            if (partName.equals("base_head") || partName.equals("eyes") || partName.equals("face") || partName.equals("facial_hair")) {
                originalWidth = originalHeight = 16;
            } else if (partName.equals("hair_back") || partName.equals("hair_mid")) {
                originalWidth = originalHeight = 40;
            } else {
                originalWidth = originalHeight = 32;
            }

            // Calculer la nouvelle taille
            int scaledWidth = (int) (originalWidth * scale);
            int scaledHeight = (int) (originalHeight * scale);

            // Effacer l'image actuelle pour forcer le rechargement
            view.setImage(null);

            // Réinitialiser les propriétés de l'ImageView AVANT de charger la nouvelle image
            view.setFitWidth(Region.USE_COMPUTED_SIZE);
            view.setFitHeight(Region.USE_COMPUTED_SIZE);
            view.setPreserveRatio(false);
            view.setSmooth(false);
        }

        // Redémarrer l'animation qui rechargera toutes les images à la bonne taille
        if (currentAction != null && !currentAction.isEmpty()) {
            setAnimation(currentAction, currentDirection);
        }
    }

    /**
     * Cleanup resources when no longer needed
     */
    public void dispose() {
        stopAllAnimations();
        spriteContainer.getChildren().removeAll(bodyParts.values());
    }

    /**
     * Updates the current frame map with provided frame data
     * @param frameMap The map of part names to frame indices
     */
    public void updateCurrentFrameMap(Map<String, Integer> frameMap) {
        currentFrameMap.clear();
        for (Map.Entry<String, Integer> entry : frameMap.entrySet()) {
            if (bodyParts.containsKey(entry.getKey())) {
                currentFrameMap.put(entry.getKey(), entry.getValue());
            }
        }
    }

    /**
     * Obtient la liste des parties disponibles pour le personnage actuel
     * @return Liste des noms des parties disponibles
     */
    public List<String> getAvailableBodyParts() {
        return spriteLoader.getAvailableBodyParts();
    }
}