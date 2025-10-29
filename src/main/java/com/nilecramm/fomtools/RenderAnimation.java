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

/**
 * Handles the rendering and animation of character sprites.
 * Manages timeline animations for different body parts and applies LUT transformations.
 */
public class RenderAnimation {
    // Default sprite sizes
    private static final int DEFAULT_SPRITE_SIZE = 32;
    private static final int HEAD_SPRITE_SIZE = 16;
    private static final int HAIR_SPRITE_SIZE = 40;
    
    private final LUTManager lutManager = new LUTManager();
    private final Map<String, Integer> currentFrameMap = new HashMap<>();

    public interface FrameUpdateListener {
        void onFrameUpdate(String partName, int frameIndex);
    }

    private FrameUpdateListener frameUpdateListener;
    private boolean isPaused = false;
    private double scale = 1.0;
    private double speed = 1.0;

    private final SpriteLoader spriteLoader = new SpriteLoader();
    
    public void setFrameUpdateListener(FrameUpdateListener listener) {
        this.frameUpdateListener = listener;
    }
    
    public LUTManager getLUTManager() {
        return lutManager;
    }
    
    public SpriteLoader getSpriteLoader() {
        return this.spriteLoader;
    }
    
    public boolean isPaused() {
        return isPaused;
    }
    private final JsonData jsonData;
    private final Pane spriteContainer;

    // Current animation state
    private String currentAction = "idle";
    private String currentDirection = "south";

    // ImageViews for each body part
    private final Map<String, ImageView> bodyParts = new HashMap<>();

    // Current animation timelines for each body part
    private final Map<String, Timeline> timelines = new HashMap<>();

    // Complete list of body parts
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

    /**
     * Loads sprites for a specific character
     * @param characterPath path to the character folder
     * @return true if loading was successful
     */
    public boolean loadCharacter(String characterPath) {
        spriteLoader.loadCharacterSprites(characterPath);
        boolean hasBasicSprites = spriteLoader.hasBasicSprites();

        if (hasBasicSprites && currentAction != null && !currentAction.isEmpty()) {
            boolean wasPaused = isPaused;
            isPaused = false;
            setAnimation(currentAction, currentDirection);

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
     * Starts animation for all available body parts with current action and direction
     */
    private void startAnimations() {
        List<String> availableParts = spriteLoader.getAvailableBodyParts();

        for (String partName : availableParts) {
            startAnimation(partName);
        }

        applyRenderOrder();
    }

    /**
     * Applies the proper render order based on current direction.
     * In JavaFX, lower viewOrder values appear in front.
     */
    private void applyRenderOrder() {
        List<String> order = renderOrder.getOrDefault(currentDirection, renderOrder.get("south"));

        for (ImageView view : bodyParts.values()) {
            view.setViewOrder(0);
        }

        for (int i = 0; i < order.size(); i++) {
            String part = order.get(i);
            if (bodyParts.containsKey(part)) {
                bodyParts.get(part).setViewOrder(i);
            }
        }
    }

    /**
     * Starts animation for a specific body part
     * @param partName the body part to animate
     */
    @SuppressWarnings("unchecked")
    private void startAnimation(String partName) {
        Map<String, ActionWrapper> partActions = getPartActions(partName);
        if (partActions == null) {
            return;
        }

        if (!partActions.containsKey(currentAction)) {
            System.out.println("Action " + currentAction + " not found for " + partName);
            return;
        }

        ActionWrapper action = partActions.get(currentAction);
        Frame[] frames = getFramesForDirection(action);

        if (frames == null || frames.length == 0) {
            System.out.println("No frames found for " + partName + " in direction " + currentDirection);
            return;
        }

        ImageView view = bodyParts.get(partName);
        if (view == null) {
            System.out.println("ImageView missing for " + partName);
            return;
        }

        createAndPlayTimeline(partName, frames, view);
    }
    
    /**
     * Retrieves the action map for a specific body part using reflection
     */
    @SuppressWarnings("unchecked")
    private Map<String, ActionWrapper> getPartActions(String partName) {
        try {
            Field field = jsonData.getClass().getField(partName);
            Map<String, ActionWrapper> partActions = (Map<String, ActionWrapper>) field.get(jsonData);
            
            if (partActions == null) {
                System.out.println("No actions found for part " + partName);
            }
            return partActions;
        } catch (Exception e) {
            System.out.println("Error accessing part " + partName + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets the frames for the current direction from an action
     */
    private Frame[] getFramesForDirection(ActionWrapper action) {
        switch (currentDirection) {
            case "north": return action.north;
            case "east": return action.east;
            case "south": return action.south;
            default: return action.south;
        }
    }
    
    /**
     * Creates and plays the timeline animation for a body part
     */
    private void createAndPlayTimeline(String partName, Frame[] frames, ImageView view) {
        Timeline timeline = new Timeline();

        double totalDuration = 0;
        for (Frame frame : frames) {
            final int frameIndex = frame.target_frame;
            final int[] offset = frame.offset.clone();

            offset[0] = (int)(offset[0] * scale);
            offset[1] = (int)(offset[1] * scale);

            double adjustedDuration = frame.duration / speed;

            KeyFrame keyFrame = new KeyFrame(
                    Duration.seconds(totalDuration),
                    e -> renderFrame(partName, frameIndex, offset, view)
            );

            timeline.getKeyFrames().add(keyFrame);
            totalDuration += adjustedDuration;
        }

        timeline.getKeyFrames().add(new KeyFrame(Duration.seconds(totalDuration)));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();

        timelines.put(partName, timeline);
    }
    
    /**
     * Renders a single frame for a body part
     */
    private void renderFrame(String partName, int frameIndex, int[] offset, ImageView view) {
        currentFrameMap.put(partName, frameIndex);

        String imagePath = spriteLoader.getSpritePath(partName, frameIndex);
        if (imagePath != null) {
            int[] spriteSize = getSpriteSize(partName);
            int scaledWidth = (int) (spriteSize[0] * scale);
            int scaledHeight = (int) (spriteSize[1] * scale);

            Image image = new Image(imagePath, scaledWidth, scaledHeight, false, false);
            Image finalImage = lutManager.applyLUT(partName, image);

            view.setImage(finalImage);
            view.setTranslateX(offset[0]);
            view.setTranslateY(offset[1]);

            if (frameUpdateListener != null) {
                frameUpdateListener.onFrameUpdate(partName, frameIndex);
            }
        } else {
            view.setImage(null);
        }
    }
    
    /**
     * Gets the original sprite size for a specific body part
     */
    private int[] getSpriteSize(String partName) {
        if (partName.equals("base_head") || partName.equals("eyes") || 
            partName.equals("face") || partName.equals("facial_hair")) {
            return new int[]{HEAD_SPRITE_SIZE, HEAD_SPRITE_SIZE};
        } else if (partName.equals("hair_back") || partName.equals("hair_mid")) {
            return new int[]{HAIR_SPRITE_SIZE, HAIR_SPRITE_SIZE};
        } else {
            return new int[]{DEFAULT_SPRITE_SIZE, DEFAULT_SPRITE_SIZE};
        }
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
     * Sets the scale of the animation
     * @param scale the scale factor
     */
    public void setScale(double scale) {
        this.scale = scale;

        for (ImageView view : bodyParts.values()) {
            view.setImage(null);
            view.setFitWidth(Region.USE_COMPUTED_SIZE);
            view.setFitHeight(Region.USE_COMPUTED_SIZE);
            view.setPreserveRatio(false);
            view.setSmooth(false);
        }

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
     * @param frameMap the map of part names to frame indices
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
     * Gets the list of available parts for the current character
     * @return list of available part names
     */
    public List<String> getAvailableBodyParts() {
        return spriteLoader.getAvailableBodyParts();
    }
}