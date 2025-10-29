package com.nilecramm.fomtools;

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
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;
import javafx.stage.Screen;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AnimationViewer extends Application {

    private ConfigManager configManager;
    private String customEditorPath = null;
    private RenderAnimation animation;
    private double scale = 2.0;
    private double speed = 1.0;
    private FlowPane spritePartsContainer;
    private Map<String, Integer> currentFrameMap = new ConcurrentHashMap<>();

    // Base path for character sprites
    private String charactersBasePath = "";

    private final Map<String, Integer> selectedLUTColors = new HashMap<>();

    private LUTGroupManager groupManager = new LUTGroupManager();
    private ComboBox<String> groupSelectionComboBox;


    @Override
    public void start(Stage primaryStage) {
        try {
            configManager = new ConfigManager();
            charactersBasePath = configManager.getCharactersPath();
            customEditorPath = configManager.getCustomEditorPath();

            MenuBar menuBar = createMenuBar(primaryStage);
            JsonData jsonData = loadJsonData();

            if (jsonData == null || jsonData.base_arm_left == null) {
                showMissingFileWindow(primaryStage, menuBar);
                return;
            }

            initializeNormalView(primaryStage, menuBar, jsonData);

        } catch (Exception e) {
            e.printStackTrace();
            showMissingFileWindow(primaryStage, createMenuBar(primaryStage));
        }
    }
    
    /**
     * Loads the JSON animation data file
     * @return the loaded JsonData, or null if loading failed
     */
    private JsonData loadJsonData() {
        try {
            ReadJson readJson = new ReadJson();
            return readJson.readJson("par_output.json");
        } catch (Exception e) {
            return null;
        }
    }
    /**
     * Opens a dialog to select the characters folder
     */
    private void selectCharactersFolder(Stage primaryStage) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Select Characters Folder");

        File initialDirectory = new File(charactersBasePath);
        if (initialDirectory.exists()) {
            directoryChooser.setInitialDirectory(initialDirectory);
        }

        File selectedDirectory = directoryChooser.showDialog(primaryStage);
        if (selectedDirectory != null) {
            charactersBasePath = selectedDirectory.getAbsolutePath() + File.separator;
            configManager.setCharactersPath(charactersBasePath);
            refreshCharacterList();
            showAlert("File Selection",
                    "File defined: " + charactersBasePath,
                    Alert.AlertType.INFORMATION);
        }
    }
    
    /**
     * Opens a dialog to select a custom image editor
     */
    private void selectCustomEditor() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select Image Editor Application");

        if (System.getProperty("os.name").toLowerCase().contains("mac")) {
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Applications", "*.app")
            );
        } else if (System.getProperty("os.name").toLowerCase().contains("win")) {
            fileChooser.getExtensionFilters().add(
                    new javafx.stage.FileChooser.ExtensionFilter("Executables", "*.exe")
            );
        }

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            customEditorPath = selectedFile.getAbsolutePath();
            configManager.setCustomEditorPath(customEditorPath);
            showAlert("Editor Defined",
                    "Custom editor set to: " + customEditorPath,
                    Alert.AlertType.INFORMATION);
        }
    }


    /**
     * Shows a window indicating that par_output.json is missing
     */
    private void showMissingFileWindow(Stage primaryStage, MenuBar menuBar) {
        // Create error content
        VBox errorContent = new VBox(20);
        errorContent.setAlignment(Pos.CENTER);
        errorContent.setPadding(new Insets(50));

        Label errorLabel = new Label("par_output.json file missing");
        errorLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: red;");

        Label instructionLabel = new Label("Please place the par_output.json file in the project root directory.");
        instructionLabel.setStyle("-fx-font-size: 14px;");
        instructionLabel.setWrapText(true);
        instructionLabel.setMaxWidth(400);

        Button retryButton = new Button("Retry");
        retryButton.setStyle("-fx-font-size: 14px; -fx-padding: 10px 20px;");
        retryButton.setOnAction(e -> {
            // Try to restart the application
            try {
                start(primaryStage);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        errorContent.getChildren().addAll(errorLabel, instructionLabel, retryButton);

        // Layout
        BorderPane root = new BorderPane();
        root.setTop(menuBar);
        root.setCenter(errorContent);

        Scene scene = new Scene(root, 600, 400);
        primaryStage.setTitle("Animation Viewer - Error");
        primaryStage.setScene(scene);
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    /**
     * Initializes the normal view when par_output.json is found
     */
    private void initializeNormalView(Stage primaryStage, MenuBar menuBar, JsonData jsonData) {
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
        refreshButton.setOnAction(e -> refreshCharacterAnimation(
                characterComboBox, actionComboBox, directionComboBox, pauseButton, detectedPartsLabel));

        // Scrolling area for part information
        ScrollPane detectedPartsScroll = new ScrollPane(detectedPartsLabel);
        detectedPartsScroll.setFitToWidth(true);
        detectedPartsScroll.setPrefHeight(100);

        VBox groupControls = createGroupControls();

        // Create control panels
        VBox animationControls = new VBox(10,
                new Label("Character:"), characterComboBox,
                new Label("Action:"), actionComboBox,
                new Label("Direction:"), directionComboBox,
                new Label("Scale:"), scaleSlider,
                new Label("Speed:"), speedSlider,
                refreshButton,
                pauseButton,
                new Separator(),
                groupControls,
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

        primaryStage.show();
    }
    
    /**
     * Refreshes the character animation, preserving pause state
     */
    private void refreshCharacterAnimation(ComboBox<String> characterComboBox, 
                                          ComboBox<String> actionComboBox, 
                                          ComboBox<String> directionComboBox, 
                                          Button pauseButton, 
                                          Label detectedPartsLabel) {
        boolean wasPaused = animation.isPaused();

        if (!wasPaused) {
            animation.togglePause();
        }

        String characterPath = charactersBasePath + characterComboBox.getValue();
        boolean loaded = animation.loadCharacter(characterPath);
        if (loaded) {
            animation.setAnimation(actionComboBox.getValue(), directionComboBox.getValue());
            updateDetectedPartsLabel(detectedPartsLabel);

            if (wasPaused || animation.isPaused()) {
                animation.renderCurrentFrame();
                updatePartsPreviews();
            }

            if (wasPaused && !animation.isPaused()) {
                animation.togglePause();
                pauseButton.setText("Resume");
            }

            if (!wasPaused && animation.isPaused()) {
                animation.togglePause();
                pauseButton.setText("Pause");
            }
        }
    }

    /**
     * Creates the group controls UI section
     */
    private VBox createGroupControls() {
        VBox groupControls = new VBox(10);

        Label groupLabel = new Label("LUT Groups:");
        groupLabel.setStyle("-fx-font-weight: bold;");

        groupSelectionComboBox = new ComboBox<>();
        groupSelectionComboBox.setMaxWidth(Double.MAX_VALUE);
        groupSelectionComboBox.setPromptText("Select a group...");
        updateGroupSelectionComboBox();

        Button loadGroupLUTButton = new Button("Load Group LUT");
        loadGroupLUTButton.setMaxWidth(Double.MAX_VALUE);
        loadGroupLUTButton.setOnAction(e -> loadLUTForSelectedGroup());

        final ComboBox<String>[] groupVariantComboBox = new ComboBox[1];

        Button removeGroupLUTButton = new Button("Remove Group LUT");
        removeGroupLUTButton.setMaxWidth(Double.MAX_VALUE);
        removeGroupLUTButton.setOnAction(e -> removeGroupLUT());

        Button refreshGroupLUTButton = new Button("Refresh LUT");
        refreshGroupLUTButton.setMaxWidth(Double.MAX_VALUE);
        refreshGroupLUTButton.setOnAction(e -> refreshGroupLUT());
        groupSelectionComboBox.setOnAction(e -> {
            String selectedGroup = groupSelectionComboBox.getValue();

            if (groupVariantComboBox[0] != null) {
                groupControls.getChildren().remove(groupVariantComboBox[0]);
            }

            if (selectedGroup != null && groupManager.hasGroup(selectedGroup)) {
                groupVariantComboBox[0] = createVariantComboBoxWithPreviews(selectedGroup);
                updateGroupVariantComboBox(groupVariantComboBox[0], selectedGroup);

                groupVariantComboBox[0].setOnAction(event -> {
                    String selectedVariant = groupVariantComboBox[0].getValue();
                    if (selectedGroup != null && selectedVariant != null) {
                        applyGroupVariant(selectedGroup, selectedVariant);
                    }
                });

                int insertIndex = groupControls.getChildren().indexOf(loadGroupLUTButton) + 1;
                groupControls.getChildren().add(insertIndex, groupVariantComboBox[0]);
            }

            boolean hasGroup = selectedGroup != null && !selectedGroup.isEmpty();
            loadGroupLUTButton.setDisable(!hasGroup);
            removeGroupLUTButton.setDisable(!hasGroup || !groupHasLUT(selectedGroup));
            refreshGroupLUTButton.setDisable(!hasGroup || !groupHasLUT(selectedGroup));
        });

        loadGroupLUTButton.setDisable(true);
        removeGroupLUTButton.setDisable(true);
        refreshGroupLUTButton.setDisable(true);

        groupControls.getChildren().addAll(
                groupLabel,
                groupSelectionComboBox,
                loadGroupLUTButton,
                removeGroupLUTButton,
                refreshGroupLUTButton
        );

        return groupControls;
    }

    private void updateGroupSelectionComboBox() {
        if (groupSelectionComboBox != null) {
            String currentSelection = groupSelectionComboBox.getValue();
            groupSelectionComboBox.setItems(groupManager.getGroupNames());

            // Restaurer la sélection si elle existe encore
            if (currentSelection != null && groupManager.hasGroup(currentSelection)) {
                groupSelectionComboBox.setValue(currentSelection);
            }
        }
    }

    private void updateGroupVariantComboBox(ComboBox<String> variantComboBox, String groupName) {
        variantComboBox.getItems().clear();

        if (groupName == null || !groupManager.hasGroup(groupName)) {
            variantComboBox.setDisable(true);
            return;
        }

        LUTGroupManager.LUTGroup group = groupManager.getGroup(groupName);
        if (group.getLutPath() == null) {
            variantComboBox.setDisable(true);
            return;
        }

        // Prendre n'importe quelle partie du groupe pour obtenir les variantes disponibles
        String samplePart = group.getParts().iterator().next();
        if (animation.getLUTManager().hasLUT(samplePart)) {
            List<Integer> availableIndices = animation.getLUTManager().getAvailableVariantIndices(samplePart);
            List<String> variantOptions = new ArrayList<>();

            for (Integer index : availableIndices) {
                if (index == 0) {
                    variantOptions.add("Base");
                } else {
                    variantOptions.add("Variant " + index);
                }
            }

            variantComboBox.setItems(FXCollections.observableArrayList(variantOptions));
            variantComboBox.setDisable(false);

            // Sélectionner la variante actuelle du groupe
            int currentVariant = group.getSelectedVariant();
            String currentLabel = currentVariant == 0 ? "Base" : "Variant " + currentVariant;
            if (variantOptions.contains(currentLabel)) {
                variantComboBox.setValue(currentLabel);
            }
        } else {
            variantComboBox.setDisable(true);
        }
    }

    /**
     * Loads a LUT file for the selected group
     */
    private void loadLUTForSelectedGroup() {
        String selectedGroup = groupSelectionComboBox.getValue();
        if (selectedGroup == null) return;

        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select LUT file for group " + selectedGroup);
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PNG Images", "*.png")
        );

        File selectedFile = fileChooser.showOpenDialog(spritePartsContainer.getScene().getWindow());
        if (selectedFile != null) {
            groupManager.setGroupLUT(selectedGroup, selectedFile.getAbsolutePath(), animation.getLUTManager());
            animation.renderCurrentFrame();
            updatePartsPreviews();
            updateGroupSelectionComboBox();
        }
    }

    /**
     * Applies a color variant to all parts in a group
     */
    private void applyGroupVariant(String groupName, String variantLabel) {
        if (!groupManager.hasGroup(groupName)) {
            return;
        }

        int variantIndex = variantLabel.equals("Base") ? 0 :
                Integer.parseInt(variantLabel.replace("Variant ", ""));

        groupManager.setGroupVariant(groupName, variantIndex, animation.getLUTManager());
        animation.renderCurrentFrame();
        updatePartsPreviews();
    }

    /**
     * Removes the LUT from the selected group
     */
    private void removeGroupLUT() {
        String selectedGroup = groupSelectionComboBox.getValue();
        if (selectedGroup == null) return;

        LUTGroupManager.LUTGroup group = groupManager.getGroup(selectedGroup);
        if (group != null) {
            for (String part : group.getParts()) {
                animation.getLUTManager().removeLUT(part);
            }

            group.setLutPath(null);
            group.setSelectedVariant(0);

            animation.renderCurrentFrame();
            updatePartsPreviews();
            updateGroupSelectionComboBox();
        }
    }

    /**
     * Refreshes the LUT for the selected group from disk
     */
    private void refreshGroupLUT() {
        String selectedGroup = groupSelectionComboBox.getValue();
        if (selectedGroup == null) return;

        LUTGroupManager.LUTGroup group = groupManager.getGroup(selectedGroup);
        if (group != null && group.getLutPath() != null) {
            groupManager.refreshGroupLUT(selectedGroup, animation.getLUTManager());
            animation.renderCurrentFrame();
            updatePartsPreviews();
            updateGroupSelectionComboBox();
        }
    }

    /**
     * Checks if a group has a LUT loaded
     */
    private boolean groupHasLUT(String groupName) {
        if (!groupManager.hasGroup(groupName)) return false;

        LUTGroupManager.LUTGroup group = groupManager.getGroup(groupName);
        return group.getLutPath() != null;
    }

    /**
     * Creates a visual preview of a color variant
     */
    private ImageView createVariantPreview(String partName, int variantIndex) {
        int previewWidth = 80;
        int previewHeight = 16;

        WritableImage previewImage = new WritableImage(previewWidth, previewHeight);
        PixelWriter writer = previewImage.getPixelWriter();

        try {
            String groupName = groupManager.getPartGroup(partName);
            if (groupName != null) {
                LUTGroupManager.LUTGroup group = groupManager.getGroup(groupName);
                if (group != null && group.getLutPath() != null) {
                    Image lutImage = new Image(new File(group.getLutPath()).toURI().toString());
                    PixelReader lutReader = lutImage.getPixelReader();

                    int lutHeight = (int) lutImage.getHeight();
                    int lutWidth = (int) lutImage.getWidth();

                    if (variantIndex < lutWidth) {
                        List<Color> validColors = new ArrayList<>();

                        for (int y = 0; y < lutHeight; y++) {
                            Color lutColor = lutReader.getColor(variantIndex, y);

                            if (lutColor.getOpacity() > 0.1 &&
                                    (lutColor.getRed() > 0.05 || lutColor.getGreen() > 0.05 || lutColor.getBlue() > 0.05)) {
                                validColors.add(lutColor);
                            }
                        }

                        if (!validColors.isEmpty()) {
                            int colorWidth = previewWidth / validColors.size();

                            for (int i = 0; i < validColors.size(); i++) {
                                Color color = validColors.get(i);
                                int startX = i * colorWidth;
                                int endX = Math.min(startX + colorWidth, previewWidth);

                                for (int x = startX; x < endX; x++) {
                                    for (int y = 0; y < previewHeight; y++) {
                                        writer.setColor(x, y, color);
                                    }
                                }
                            }
                        } else {
                            fillWithGradient(writer, previewWidth, previewHeight);
                        }
                    } else {
                        fillWithGradient(writer, previewWidth, previewHeight);
                    }
                } else {
                    fillWithGradient(writer, previewWidth, previewHeight);
                }
            } else {
                fillWithGradient(writer, previewWidth, previewHeight);
            }
        } catch (Exception e) {
            fillWithGradient(writer, previewWidth, previewHeight);
        }

        ImageView previewView = new ImageView(previewImage);
        previewView.setFitWidth(80);
        previewView.setFitHeight(16);
        previewView.setPreserveRatio(false);
        previewView.setSmooth(false);

        return previewView;
    }

    /**
     * Fills an image with a gray gradient
     */
    private void fillWithGradient(PixelWriter writer, int width, int height) {
        for (int x = 0; x < width; x++) {
            double intensity = 0.7 + (0.2 * x / width);
            Color grayColor = Color.gray(intensity);
            for (int y = 0; y < height; y++) {
                writer.setColor(x, y, grayColor);
            }
        }
    }

    /**
     * Creates a custom ComboBox that displays color variant previews
     */
    private ComboBox<String> createVariantComboBoxWithPreviews(String groupName) {
        ComboBox<String> variantComboBox = new ComboBox<>();
        variantComboBox.setMaxWidth(Double.MAX_VALUE);
        variantComboBox.setPromptText("Select variant...");

        // Créer un cell factory personnalisé pour afficher les aperçus
        variantComboBox.setCellFactory(param -> new ListCell<String>() {
            private final HBox content = new HBox(10);
            private final Label textLabel = new Label();
            private final ImageView preview = new ImageView();

            {
                content.setAlignment(Pos.CENTER_LEFT);
                content.getChildren().addAll(textLabel, preview);
                textLabel.setStyle("-fx-text-fill: black;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    textLabel.setText(item);

                    int variantIndex = item.equals("Base") ? 0 :
                            Integer.parseInt(item.replace("Variant ", ""));

                    LUTGroupManager.LUTGroup group = groupManager.getGroup(groupName);
                    if (group != null && !group.getParts().isEmpty()) {
                        String samplePart = group.getParts().iterator().next();
                        ImageView previewImage = createVariantPreview(samplePart, variantIndex);
                        preview.setImage(previewImage.getImage());
                        preview.setFitWidth(60);
                        preview.setFitHeight(12);
                        preview.setPreserveRatio(false);
                        preview.setSmooth(false);
                    }

                    setGraphic(content);
                    setText(null);
                    setStyle("-fx-text-fill: black;");
                }
            }
        });

        variantComboBox.setButtonCell(new ListCell<String>() {
            private final HBox content = new HBox(10);
            private final Label textLabel = new Label();
            private final ImageView preview = new ImageView();

            {
                content.setAlignment(Pos.CENTER_LEFT);
                content.getChildren().addAll(textLabel, preview);
                textLabel.setStyle("-fx-text-fill: black;");
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    textLabel.setText(item);

                    int variantIndex = item.equals("Base") ? 0 :
                            Integer.parseInt(item.replace("Variant ", ""));

                    LUTGroupManager.LUTGroup group = groupManager.getGroup(groupName);
                    if (group != null && !group.getParts().isEmpty()) {
                        String samplePart = group.getParts().iterator().next();
                        ImageView previewImage = createVariantPreview(samplePart, variantIndex);
                        preview.setImage(previewImage.getImage());
                        preview.setFitWidth(40);
                        preview.setFitHeight(8);
                        preview.setPreserveRatio(false);
                        preview.setSmooth(false);
                    }

                    setGraphic(content);
                    setText(null);
                    setStyle("-fx-text-fill: black;");
                }
            }
        });

        return variantComboBox;
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

        // Manage LUT Groups option
        MenuItem manageLUTGroupsMenuItem = new MenuItem("Manage LUT Groups");
        manageLUTGroupsMenuItem.setOnAction(e -> openGroupManagerWindow());

        // Add items to Edit menu
        editMenu.getItems().addAll(setEditorMenuItem, setCharactersFolderMenuItem, new SeparatorMenuItem(), manageLUTGroupsMenuItem);

        // Add menus to menuBar
        menuBar.getMenus().add(editMenu);

        return menuBar;
    }

    /**
     * Opens the group manager window
     */
    private void openGroupManagerWindow() {
        List<String> availableParts = animation.getAvailableBodyParts();
        GroupManagerWindow groupWindow = new GroupManagerWindow(groupManager, availableParts);

        groupWindow.setOnGroupsChangedCallback(this::updateGroupSelectionComboBox);
        groupWindow.show();
    }

    /**
     * Refreshes the list of available characters in the ComboBox
     */
    private void refreshCharacterList() {
        Scene scene = ((Stage) spritePartsContainer.getScene().getWindow()).getScene();
        ComboBox<String> characterComboBox = null;

        for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox) {
                characterComboBox = (ComboBox<String>) node;
                break;
            }
        }

        if (characterComboBox != null) {
            String currentSelection = characterComboBox.getValue();
            List<String> characters = SpriteLoader.listCharacterFolders(charactersBasePath);
            characterComboBox.setItems(FXCollections.observableArrayList(characters));

            if (characters.contains(currentSelection)) {
                characterComboBox.setValue(currentSelection);
            } else if (!characters.isEmpty()) {
                characterComboBox.setValue(characters.get(0));
                String characterPath = charactersBasePath + characters.get(0);
                boolean loaded = animation.loadCharacter(characterPath);
                if (loaded) {
                    ComboBox<String> actionComboBox = findActionComboBox(scene);
                    ComboBox<String> directionComboBox = findDirectionComboBox(scene);

                    if (actionComboBox != null && directionComboBox != null) {
                        animation.setAnimation(actionComboBox.getValue(), directionComboBox.getValue());
                    }

                    updateAllLabels(scene);
                }
            }
        }
    }

    /**
     * Finds the action ComboBox in the scene (assumes it's the second one)
     */
    private ComboBox<String> findActionComboBox(Scene scene) {
        List<ComboBox> comboBoxes = new ArrayList<>();
        for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox) {
                comboBoxes.add((ComboBox) node);
            }
        }
        return comboBoxes.size() > 1 ? comboBoxes.get(1) : null;
    }

    /**
     * Finds the direction ComboBox in the scene (assumes it's the third one)
     */
    private ComboBox<String> findDirectionComboBox(Scene scene) {
        List<ComboBox> comboBoxes = new ArrayList<>();
        for (Node node : scene.getRoot().lookupAll(".combo-box")) {
            if (node instanceof ComboBox) {
                comboBoxes.add((ComboBox) node);
            }
        }
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

    /**
     * Updates the sprite parts preview panel
     */
    private void updatePartsPreviews() {
        spritePartsContainer.getChildren().clear();

        for (String partName : currentFrameMap.keySet()) {
            int frameIndex = currentFrameMap.get(partName);
            String imagePath = animation.getSpriteLoader().getSpritePath(partName, frameIndex);

            if (imagePath != null) {
                VBox partBox = new VBox(5);
                partBox.setAlignment(Pos.CENTER);
                partBox.setStyle("-fx-border-color: lightgray; -fx-border-width: 1px; -fx-padding: 5px;");
                partBox.setPrefWidth(120);
                partBox.setPrefHeight(180);

                Image originalImage = new Image(imagePath, 80, 80, false, false);
                Image finalImage = animation.getLUTManager().applyLUT(partName, originalImage);

                ImageView spriteView = new ImageView(finalImage);
                spriteView.setFitWidth(Region.USE_COMPUTED_SIZE);
                spriteView.setFitHeight(Region.USE_COMPUTED_SIZE);
                spriteView.setPreserveRatio(false);
                spriteView.setSmooth(false);

                Label nameLabel = new Label(partName);
                nameLabel.setWrapText(true);
                nameLabel.setMaxWidth(110);
                nameLabel.setAlignment(Pos.CENTER);

                Label frameLabel = new Label("Frame: " + frameIndex);
                frameLabel.setStyle("-fx-font-weight: bold;");
                frameLabel.setAlignment(Pos.CENTER);

                String partGroup = groupManager.getPartGroup(partName);
                if (partGroup != null) {
                    Label groupLabel = new Label("Group: " + partGroup);
                    groupLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: blue;");
                    groupLabel.setAlignment(Pos.CENTER);
                    partBox.getChildren().add(groupLabel);
                }

                Button editButton = new Button("Edit");
                editButton.setOnAction(e -> openSpriteInExternalEditor(imagePath));

                partBox.getChildren().addAll(spriteView, nameLabel, frameLabel, editButton);
                spritePartsContainer.getChildren().add(partBox);
            }
        }
    }

    /**
     * Updates a single sprite image in the preview without recreating everything
     */
    private void updateSinglePartPreview(String partName, int frameIndex, ImageView spriteView) {
        String imagePath = animation.getSpriteLoader().getSpritePath(partName, frameIndex);
        if (imagePath != null) {
            Image originalImage = new Image(imagePath, 80, 80, false, false);
            Image finalImage = animation.getLUTManager().applyLUT(partName, originalImage);
            spriteView.setImage(finalImage);
        }
    }

    /**
     * Loads a LUT file for a specific body part
     */
    private void loadLUTForPart(String partName) {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Select LUT file for " + partName);
        fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("PNG Images", "*.png")
        );

        File selectedFile = fileChooser.showOpenDialog(spritePartsContainer.getScene().getWindow());
        if (selectedFile != null) {
            boolean success = animation.getLUTManager().loadLUT(partName, selectedFile.getAbsolutePath());
            if (success) {
                animation.renderCurrentFrame();
                updatePartsPreviews();
            } else {
                showAlert("LUT Error",
                        "Unable to load LUT file for " + partName,
                        Alert.AlertType.ERROR);
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