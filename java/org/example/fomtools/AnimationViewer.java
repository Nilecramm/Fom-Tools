package org.example.fomtools;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationViewer extends Application {

    private String customEditorPath = null;
    private RenderAnimation animation;
    private double scale = 2.0;
    private double speed = 1.0;
    private FlowPane spritePartsContainer;
    private Map<String, Integer> currentFrameMap = new ConcurrentHashMap<>();

    // Base path for character sprites
    private String charactersBasePath = "";

    @Override
    public void start(Stage primaryStage) {
        try {
            // Create menu bar
            MenuBar menuBar = createMenuBar(primaryStage);

            // Load animations
            ReadJson readJson = new ReadJson();
            JsonData jsonData = readJson.readJson("par_output.json");

            // Get available actions and directions
            ArrayList<String> actions = new ArrayList<>(jsonData.base_arm_left.keySet());
            ArrayList<String> directions = new ArrayList<>();
            directions.add("north");
            directions.add("south");
            directions.add("east");

            // Animation container
            Pane animationContainer = new Pane();
            animation = new RenderAnimation(jsonData, animationContainer);

            // Set a frame tracker on the animation
            animation.setFrameUpdateListener((partName, frameIndex) -> {
                currentFrameMap.put(partName, frameIndex);
                updatePartsPreviews();
            });

            // Set initial animation
            String initialAction = actions.get(0);
            String initialDirection = "south";
            animation.setScale(scale);
            animation.setSpeed(speed);

            // Create animation view with border
            StackPane animationView = new StackPane(animationContainer);
            animationView.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px;");
            animationView.setPrefSize(300, 300);
            animationView.setMinSize(300, 300);

            // Sprite parts preview container
            spritePartsContainer = new FlowPane();
            spritePartsContainer.setPadding(new Insets(10));
            spritePartsContainer.setHgap(10);
            spritePartsContainer.setVgap(10);
            spritePartsContainer.setPrefWrapLength(580);

            // Wrap in a scroll pane
            ScrollPane spritePartsScroll = new ScrollPane(spritePartsContainer);
            spritePartsScroll.setFitToWidth(true);
            spritePartsScroll.setPrefHeight(200);

            // Get the list of available characters
            List<String> characters = SpriteLoader.listCharacterFolders(charactersBasePath);
            ComboBox<String> characterComboBox = new ComboBox<>(FXCollections.observableArrayList(characters));
            characterComboBox.setMaxWidth(Double.MAX_VALUE);

            // Label to display detected parts
            Label detectedPartsLabel = new Label("Detected parts: No character loaded");

            if (!characters.isEmpty()) {
                characterComboBox.setValue(characters.get(0));
                String characterPath = charactersBasePath + characterComboBox.getValue();
                boolean loaded = animation.loadCharacter(characterPath);
                if (loaded) {
                    animation.setAnimation(initialAction, initialDirection);
                    updateDetectedPartsLabel(detectedPartsLabel);
                }
            }

            // Create controls for animation
            ComboBox<String> actionComboBox = new ComboBox<>(FXCollections.observableArrayList(actions));
            actionComboBox.setValue(initialAction);
            actionComboBox.setMaxWidth(Double.MAX_VALUE);

            ComboBox<String> directionComboBox = new ComboBox<>(FXCollections.observableArrayList(directions));
            directionComboBox.setValue(initialDirection);
            directionComboBox.setMaxWidth(Double.MAX_VALUE);

            // Add change listeners
            characterComboBox.setOnAction(e -> {
                String characterPath = charactersBasePath + characterComboBox.getValue();
                boolean loaded = animation.loadCharacter(characterPath);
                if (loaded) {
                    animation.setAnimation(actionComboBox.getValue(), directionComboBox.getValue());
                    updateDetectedPartsLabel(detectedPartsLabel);
                } else {
                    showAlert("Incomplete Character",
                            "Unable to load the necessary base sprites for this character.",
                            Alert.AlertType.WARNING);
                }
            });

            actionComboBox.setOnAction(e ->
                    animation.setAnimation(actionComboBox.getValue(), directionComboBox.getValue()));

            directionComboBox.setOnAction(e ->
                    animation.setAnimation(actionComboBox.getValue(), directionComboBox.getValue()));

            // Scale controls
            Slider scaleSlider = new Slider(0.5, 5.0, scale);
            scaleSlider.setShowTickMarks(true);
            scaleSlider.setShowTickLabels(true);
            scaleSlider.setMajorTickUnit(0.5);
            scaleSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                scale = newVal.doubleValue();
                animation.setScale(scale);
            });

            // Speed controls
            Slider speedSlider = new Slider(0.25, 3.0, speed);
            speedSlider.setShowTickMarks(true);
            speedSlider.setShowTickLabels(true);
            speedSlider.setMajorTickUnit(0.25);
            speedSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                speed = newVal.doubleValue();
                animation.setSpeed(speed);
            });

            Button pauseButton = new Button("Pause");
            pauseButton.setMaxWidth(Double.MAX_VALUE);
            pauseButton.setOnAction(e -> {
                animation.togglePause();
                pauseButton.setText(animation.isPaused() ? "Resume" : "Pause");
            });

            // Add a refresh button
            Button refreshButton = new Button("Refresh");
            refreshButton.setMaxWidth(Double.MAX_VALUE);
            refreshButton.setOnAction(e -> {
                // Save the current pause state
                boolean wasPaused = animation.isPaused();

                // Force pause if not already paused
                if (!wasPaused) {
                    animation.togglePause();
                }

                // Reload character and animation
                String characterPath = charactersBasePath + characterComboBox.getValue();
                boolean loaded = animation.loadCharacter(characterPath);
                if (loaded) {
                    animation.setAnimation(actionComboBox.getValue(), directionComboBox.getValue());
                    updateDetectedPartsLabel(detectedPartsLabel);

                    // Force render the current frame if paused
                    if (wasPaused || animation.isPaused()) {
                        animation.renderCurrentFrame();
                        // Update the sprite previews as well
                        updatePartsPreviews();
                    }

                    // Make sure it's paused again if it was paused before
                    if (wasPaused && !animation.isPaused()) {
                        animation.togglePause();
                        pauseButton.setText("Resume");
                    }

                    // If we forced pause earlier, unpause it
                    if (!wasPaused && animation.isPaused()) {
                        animation.togglePause();
                        pauseButton.setText("Pause");
                    }
                }
            });

            // Scrolling area for part information
            ScrollPane detectedPartsScroll = new ScrollPane(detectedPartsLabel);
            detectedPartsScroll.setFitToWidth(true);
            detectedPartsScroll.setPrefHeight(100);

            // Create control panels
            VBox animationControls = new VBox(10,
                    new Label("Character:"), characterComboBox,
                    new Label("Action:"), actionComboBox,
                    new Label("Direction:"), directionComboBox,
                    new Label("Scale:"), scaleSlider,
                    new Label("Speed:"), speedSlider,
                    refreshButton,
                    pauseButton,
                    new Label("Information:"),
                    detectedPartsScroll
            );
            animationControls.setPadding(new Insets(10));
            animationControls.setPrefWidth(250);

            // Layout
            BorderPane root = new BorderPane();
            root.setTop(menuBar);
            root.setCenter(new VBox(10, animationView,
                    new Label("Individual Sprites:"),
                    spritePartsScroll));
            root.setRight(animationControls);

            Scene scene = new Scene(root);
            primaryStage.setTitle("Animation Viewer");
            primaryStage.setScene(scene);

// Make the window fill most of the screen
            Screen screen = Screen.getPrimary();
            Rectangle2D bounds = screen.getVisualBounds();
            primaryStage.setWidth(bounds.getWidth());
            primaryStage.setHeight(bounds.getHeight());

// Center the window
            primaryStage.centerOnScreen();

// This is the missing line that makes the window visible
            primaryStage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates the menu bar with Edit options
     */
    private MenuBar createMenuBar(Stage primaryStage) {
        MenuBar menuBar = new MenuBar();

        // Create Edit menu
        Menu editMenu = new Menu("Edit");

        // Set custom editor option
        MenuItem setEditorMenuItem = new MenuItem("Set Custom Editor");
        setEditorMenuItem.setOnAction(e -> selectCustomEditor());

        // Set characters folder option
        MenuItem setCharactersFolderMenuItem = new MenuItem("Set Characters Folder");
        setCharactersFolderMenuItem.setOnAction(e -> selectCharactersFolder(primaryStage));

        // Add items to Edit menu
        editMenu.getItems().addAll(setEditorMenuItem, setCharactersFolderMenuItem);

        // Add menus to menuBar
        menuBar.getMenus().add(editMenu);

        return menuBar;
    }

    /**
     * Allows the user to select a new characters folder
     */
    private void selectCharactersFolder(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Characters Folder");

        // Set initial directory if possible
        File initialDirectory = new File(charactersBasePath);
        if (initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            charactersBasePath = selectedDirectory.getAbsolutePath() + File.separator;

            // Update the character list in the ComboBox
            refreshCharacterList();

            // Show confirmation
            showAlert("Folder Set",
                    "Characters folder set to: " + charactersBasePath,
                    Alert.AlertType.INFORMATION);
        }
    }

    /**
     * Refreshes the list of available characters in the ComboBox
     */
    private void refreshCharacterList() {
        // Find the ComboBox in the scene
        Scene scene = ((Stage) spritePartsContainer.getScene().getWindow()).getScene();
        ComboBox<String> characterComboBox = null;

        // Search for the ComboBox in the UI hierarchy
        for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox) {
                // This assumes the first ComboBox found is the character selection one
                characterComboBox = (ComboBox<String>) node;
                break;
            }
        }

        if (characterComboBox != null) {
            // Save the current selection if possible
            String currentSelection = characterComboBox.getValue();

            // Get the updated list of characters
            List<String> characters = SpriteLoader.listCharacterFolders(charactersBasePath);

            // Update the ComboBox items
            characterComboBox.setItems(FXCollections.observableArrayList(characters));

            // Try to restore previous selection if it still exists, otherwise select first item
            if (characters.contains(currentSelection)) {
                characterComboBox.setValue(currentSelection);
            } else if (!characters.isEmpty()) {
                characterComboBox.setValue(characters.get(0));
                // Load the first character
                String characterPath = charactersBasePath + characters.get(0);
                boolean loaded = animation.loadCharacter(characterPath);
                if (loaded) {
                    // Get the ComboBox values for action and direction
                    ComboBox<String> actionComboBox = findActionComboBox(scene);
                    ComboBox<String> directionComboBox = findDirectionComboBox(scene);

                    if (actionComboBox != null && directionComboBox != null) {
                        animation.setAnimation(actionComboBox.getValue(), directionComboBox.getValue());
                    }

                    // Update parts label
                    updateAllLabels(scene);
                }
            }
        }
    }

    /**
     * Finds the action ComboBox in the scene
     */
    private ComboBox<String> findActionComboBox(Scene scene) {
        // This is a simple implementation. You may need to adjust the logic to find the correct ComboBox.
        List<ComboBox> comboBoxes = new ArrayList<>();
        for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox) {
                comboBoxes.add((ComboBox) node);
            }
        }

        // Assuming the action ComboBox is the second one
        return comboBoxes.size() > 1 ? comboBoxes.get(1) : null;
    }

    /**
     * Finds the direction ComboBox in the scene
     */
    private ComboBox<String> findDirectionComboBox(Scene scene) {
        // This is a simple implementation. You may need to adjust the logic to find the correct ComboBox.
        List<ComboBox> comboBoxes = new ArrayList<>();
        for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox) {
                comboBoxes.add((ComboBox) node);
            }
        }

        // Assuming the direction ComboBox is the third one
        return comboBoxes.size() > 2 ? comboBoxes.get(2) : null;
    }

    /**
     * Updates all labels in the scene
     */
    private void updateAllLabels(Scene scene) {
        for (Node node : scene.getRoot().lookupAll(".label")) {
            if (node instanceof Label) {
                Label label = (Label) node;
                if (label.getText().startsWith("Detected parts")) {
                    updateDetectedPartsLabel(label);
                    break;
                }
            }
        }
    }

    private void updatePartsPreviews() {
        spritePartsContainer.getChildren().clear();

        for (String partName : currentFrameMap.keySet()) {
            int frameIndex = currentFrameMap.get(partName);
            String imagePath = animation.getSpriteLoader().getSpritePath(partName, frameIndex);

            if (imagePath != null) {
                // Create a VBox for each sprite part
                VBox partBox = new VBox(5);
                partBox.setAlignment(Pos.CENTER);
                partBox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px; -fx-padding: 5px;");
                partBox.setPrefWidth(100);
                partBox.setPrefHeight(130);

                // Create the sprite image view first
                ImageView spriteView = new ImageView(new Image(imagePath));
                spriteView.setFitHeight(80);
                spriteView.setFitWidth(80);
                spriteView.setPreserveRatio(true);
                spriteView.setSmooth(false);

                // Make the sprite clickable to open in external editor
                final String finalImagePath = imagePath;
                spriteView.setOnMouseClicked(event -> openSpriteInExternalEditor(finalImagePath));
                spriteView.setCursor(javafx.scene.Cursor.HAND); // Change cursor to indicate it's clickable

                // Create a label for the part name
                Label nameLabel = new Label(partName);
                nameLabel.setWrapText(true);
                nameLabel.setMaxWidth(90);
                nameLabel.setAlignment(Pos.CENTER);

                // Create a separate label for the frame number with larger font
                Label frameLabel = new Label("Frame: " + frameIndex);
                frameLabel.setStyle("-fx-font-weight: bold;");
                frameLabel.setAlignment(Pos.CENTER);

                // Add an edit button
                Button editButton = new Button(customEditorPath != null ? "Edit (Custom)" : "Edit (Default)");
                editButton.setOnAction(e -> openSpriteInExternalEditor(finalImagePath));

                partBox.getChildren().addAll(spriteView, nameLabel, frameLabel, editButton);
                spritePartsContainer.getChildren().add(partBox);
            }
        }
    }

    /**
     * Opens the sprite in the system's default image editor or the custom editor if set
     * @param imagePath Path to the sprite image
     */
    private void openSpriteInExternalEditor(String imagePath) {
        try {
            System.out.println("Original image path: " + imagePath);

            // Convert URL format to file path
            if (imagePath.startsWith("file:")) {
                try {
                    // Use Paths.get(URI) which handles file URIs better
                    java.net.URI uri = new java.net.URI(imagePath);
                    imagePath = new File(uri).getAbsolutePath();
                } catch (Exception e) {
                    // Multiple fallback methods for different formats
                    if (imagePath.startsWith("file:///")) {
                        imagePath = imagePath.substring(8); // Remove "file:///"
                    } else if (imagePath.startsWith("file://")) {
                        imagePath = imagePath.substring(7); // Remove "file://"
                    } else if (imagePath.startsWith("file:/")) {
                        imagePath = imagePath.substring(6); // Remove "file:/"
                    } else {
                        imagePath = imagePath.substring(5); // Remove "file:"
                    }

                    // Ensure proper path format for Windows
                    if (System.getProperty("os.name").toLowerCase().contains("win")) {
                        // Replace forward slashes with backslashes
                        imagePath = imagePath.replace("/", "\\");
                        // Add drive letter if missing
                        if (imagePath.startsWith("\\")) {
                            imagePath = "C:" + imagePath;
                        }
                    }
                }
            }

            System.out.println("Resolved path: " + imagePath);
            File file = new File(imagePath);

            if (!file.exists()) {
                System.out.println("File not found: " + file.getAbsolutePath());
                showAlert("File Not Found",
                        "The sprite file does not exist: " + imagePath,
                        Alert.AlertType.ERROR);
                return;
            }

            String os = System.getProperty("os.name").toLowerCase();
            boolean isMac = os.contains("mac");
            boolean isWindows = os.contains("win");

            System.out.println("Opening with " + (customEditorPath != null ? "custom editor: " + customEditorPath : "default editor"));

            if (customEditorPath != null && !customEditorPath.isEmpty()) {
                // Use the custom editor
                ProcessBuilder pb = new ProcessBuilder();

                if (isMac) {
                    // macOS specific handling
                    String editorName = customEditorPath;
                    if (editorName.endsWith(".app")) {
                        editorName = editorName.substring(0, editorName.length() - 4);
                    }

                    if (editorName.contains("/")) {
                        editorName = new File(editorName).getName();
                    }

                    pb.command("open", "-a", editorName, file.getAbsolutePath());
                } else if (isWindows) {
                    // Windows specific command
                    pb.command(customEditorPath, "\"" + file.getAbsolutePath() + "\"");
                } else {
                    // Linux and other OS
                    pb.command(customEditorPath, file.getAbsolutePath());
                }

                System.out.println("Executing: " + String.join(" ", pb.command()));
                Process process = pb.start();

                // Log errors
                new Thread(() -> {
                    try (java.util.Scanner s = new java.util.Scanner(process.getErrorStream()).useDelimiter("\\A")) {
                        if (s.hasNext()) {
                            System.err.println("Editor process error: " + s.next());
                        }
                    }
                }).start();
            } else {
                // Use system default editor
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                    System.out.println("Opening with Desktop.open: " + file.getAbsolutePath());
                    Desktop.getDesktop().open(file);
                } else {
                    throw new UnsupportedOperationException("Desktop not supported on this platform");
                }
            }
        } catch (Exception e) {
            System.err.println("Error opening editor: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error Opening Editor",
                    "Could not open the sprite in an editor: " + e.getMessage(),
                    Alert.AlertType.ERROR);
        }
    }

    /**
     * Shows a dialog to select a custom editor application
     */
    private void selectCustomEditor() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Image Editor Application");

        // Set filters based on OS
        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Applications", "*.app")
            );
        } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Executables", "*.exe")
            );
        }

        java.io.File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            customEditorPath = selectedFile.getAbsolutePath();
            // Show confirmation
            showAlert("Editor Set",
                    "Custom editor set to: " + customEditorPath,
                    Alert.AlertType.INFORMATION);
        }
    }

    /**
     * Updates the label with detected parts
     */
    private void updateDetectedPartsLabel(Label label) {
        List<String> parts = animation.getAvailableBodyParts();
        if (parts.isEmpty()) {
            label.setText("No parts detected");
        } else {
            StringBuilder sb = new StringBuilder("Detected parts (" + parts.size() + "):\n");
            for (String part : parts) {
                sb.append("• ").append(part).append("\n");
            }
            label.setText(sb.toString());
        }
    }

    /**
     * Displays an alert dialog
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}