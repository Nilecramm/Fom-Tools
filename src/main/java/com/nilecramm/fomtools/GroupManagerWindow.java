package com.nilecramm.fomtools;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.util.List;

public class GroupManagerWindow {
    private final LUTGroupManager groupManager;
    private final List<String> availableParts;
    private Stage window;
    private ListView<String> groupsList;
    private ListView<String> partsInGroupList;
    private ListView<String> availablePartsList;

    private Runnable onGroupsChangedCallback;

    public GroupManagerWindow(LUTGroupManager groupManager, List<String> availableParts) {
        this.groupManager = groupManager;
        this.availableParts = availableParts;
        createWindow();
    }

    public void setOnGroupsChangedCallback(Runnable callback) {
        this.onGroupsChangedCallback = callback;
    }

    private void createWindow() {
        window = new Stage();
        window.setTitle("LUT Group Manager");
        window.initModality(Modality.APPLICATION_MODAL);

        // Liste des groupes
        VBox groupsSection = new VBox(10);
        groupsSection.setPadding(new Insets(10));

        Label groupsLabel = new Label("Groups:");
        groupsLabel.setStyle("-fx-font-weight: bold;");

        groupsList = new ListView<>();
        groupsList.setPrefHeight(150);
        groupsList.getItems().addAll(groupManager.getGroupNames());

        HBox groupButtons = new HBox(5);
        Button addGroupBtn = new Button("New Group");
        Button deleteGroupBtn = new Button("Delete Group");
        deleteGroupBtn.setDisable(true);

        groupButtons.getChildren().addAll(addGroupBtn, deleteGroupBtn);
        groupsSection.getChildren().addAll(groupsLabel, groupsList, groupButtons);

        // Parties dans le groupe sélectionné
        VBox partsInGroupSection = new VBox(10);
        partsInGroupSection.setPadding(new Insets(10));

        Label partsInGroupLabel = new Label("Parts in Group:");
        partsInGroupLabel.setStyle("-fx-font-weight: bold;");

        partsInGroupList = new ListView<>();
        partsInGroupList.setPrefHeight(150);

        Button removePartBtn = new Button("Take out of group");
        removePartBtn.setDisable(true);

        partsInGroupSection.getChildren().addAll(partsInGroupLabel, partsInGroupList, removePartBtn);

        // Parties disponibles
        VBox availablePartsSection = new VBox(10);
        availablePartsSection.setPadding(new Insets(10));

        Label availablePartsLabel = new Label("Disponible parts:");
        availablePartsLabel.setStyle("-fx-font-weight: bold;");

        availablePartsList = new ListView<>();
        availablePartsList.setPrefHeight(150);
        updateAvailablePartsList();

        Button addPartBtn = new Button("Add to group");
        addPartBtn.setDisable(true);

        availablePartsSection.getChildren().addAll(availablePartsLabel, availablePartsList, addPartBtn);

        // Actions
        addGroupBtn.setOnAction(e -> createNewGroup());
        deleteGroupBtn.setOnAction(e -> deleteSelectedGroup());
        addPartBtn.setOnAction(e -> addPartToSelectedGroup());
        removePartBtn.setOnAction(e -> removePartFromSelectedGroup());

        // Listeners pour activer/désactiver les boutons
        groupsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            deleteGroupBtn.setDisable(newVal == null);
            addPartBtn.setDisable(newVal == null || availablePartsList.getSelectionModel().getSelectedItem() == null);
            updatePartsInGroupList();
        });

        partsInGroupList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            removePartBtn.setDisable(newVal == null);
        });

        availablePartsList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            addPartBtn.setDisable(newVal == null || groupsList.getSelectionModel().getSelectedItem() == null);
        });

        // Layout principal
        HBox mainLayout = new HBox(10);
        mainLayout.setPadding(new Insets(10));

        groupsSection.setPrefWidth(200);
        partsInGroupSection.setPrefWidth(200);
        availablePartsSection.setPrefWidth(200);

        mainLayout.getChildren().addAll(groupsSection, partsInGroupSection, availablePartsSection);

        // Boutons de fermeture
        HBox bottomButtons = new HBox(10);
        bottomButtons.setAlignment(Pos.CENTER_RIGHT);
        bottomButtons.setPadding(new Insets(10));

        Button closeBtn = new Button("Close");
        closeBtn.setOnAction(e -> window.close());

        bottomButtons.getChildren().add(closeBtn);

        VBox root = new VBox();
        root.getChildren().addAll(mainLayout, bottomButtons);

        Scene scene = new Scene(root, 650, 400);
        window.setScene(scene);
    }

    private void createNewGroup() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("New Group");
        dialog.setHeaderText("Create a new group");
        dialog.setContentText("Name of the group:");

        dialog.showAndWait().ifPresent(groupName -> {
            if (!groupName.trim().isEmpty() && !groupManager.hasGroup(groupName)) {
                groupManager.createGroup(groupName);
                groupsList.getItems().add(groupName);
                updateAvailablePartsList();

                if (onGroupsChangedCallback != null) {
                    onGroupsChangedCallback.run();
                }
            }
        });
    }

    private void deleteSelectedGroup() {
        String selectedGroup = groupsList.getSelectionModel().getSelectedItem();
        if (selectedGroup != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Delete Group");
            alert.setHeaderText("Are you sure?");
            alert.setContentText("Delete the group '" + selectedGroup + "' ?");

            alert.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    groupManager.deleteGroup(selectedGroup);
                    groupsList.getItems().remove(selectedGroup);
                    updatePartsInGroupList();
                    updateAvailablePartsList();

                    if (onGroupsChangedCallback != null) {
                        onGroupsChangedCallback.run();
                    }
                }
            });
        }
    }

    private void addPartToSelectedGroup() {
        String selectedGroup = groupsList.getSelectionModel().getSelectedItem();
        String selectedPart = availablePartsList.getSelectionModel().getSelectedItem();

        if (selectedGroup != null && selectedPart != null) {
            groupManager.addPartToGroup(selectedPart, selectedGroup);
            updatePartsInGroupList();
            updateAvailablePartsList();
        }
    }

    private void removePartFromSelectedGroup() {
        String selectedPart = partsInGroupList.getSelectionModel().getSelectedItem();

        if (selectedPart != null) {
            groupManager.removePartFromCurrentGroup(selectedPart);
            updatePartsInGroupList();
            updateAvailablePartsList();
        }
    }

    private void updatePartsInGroupList() {
        partsInGroupList.getItems().clear();
        String selectedGroup = groupsList.getSelectionModel().getSelectedItem();

        if (selectedGroup != null) {
            LUTGroupManager.LUTGroup group = groupManager.getGroup(selectedGroup);
            if (group != null) {
                partsInGroupList.getItems().addAll(group.getParts());
            }
        }
    }

    private void updateAvailablePartsList() {
        availablePartsList.getItems().clear();

        // Ajouter seulement les parties qui ne sont dans aucun groupe
        for (String part : availableParts) {
            if (groupManager.getPartGroup(part) == null) {
                availablePartsList.getItems().add(part);
            }
        }
    }

    public void show() {
        window.show();
    }
}