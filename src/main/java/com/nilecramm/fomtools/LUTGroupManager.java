package com.nilecramm.fomtools;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;

public class LUTGroupManager {
    private final Map<String, LUTGroup> groups = new HashMap<>();
    private final Map<String, String> partToGroup = new HashMap<>();

    public static class LUTGroup {
        private String name;
        private String lutPath;
        private int selectedVariant;
        private final Set<String> parts;

        public LUTGroup(String name) {
            this.name = name;
            this.selectedVariant = 0;
            this.parts = new HashSet<>();
        }

        // Getters et setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getLutPath() { return lutPath; }
        public void setLutPath(String lutPath) { this.lutPath = lutPath; }
        public int getSelectedVariant() { return selectedVariant; }
        public void setSelectedVariant(int selectedVariant) { this.selectedVariant = selectedVariant; }
        public Set<String> getParts() { return parts; }

        public void addPart(String part) { parts.add(part); }
        public void removePart(String part) { parts.remove(part); }
        public boolean hasPart(String part) { return parts.contains(part); }
    }

    /**
     * Crée un nouveau groupe
     */
    public void createGroup(String groupName) {
        if (!groups.containsKey(groupName)) {
            groups.put(groupName, new LUTGroup(groupName));
        }
    }

    /**
     * Supprime un groupe
     */
    public void deleteGroup(String groupName) {
        LUTGroup group = groups.get(groupName);
        if (group != null) {
            // Retirer toutes les parties de ce groupe
            for (String part : group.getParts()) {
                partToGroup.remove(part);
            }
            groups.remove(groupName);
        }
    }

    /**
     * Ajoute une partie à un groupe
     */
    public void addPartToGroup(String partName, String groupName) {
        // Retirer de l'ancien groupe si nécessaire
        removePartFromCurrentGroup(partName);

        LUTGroup group = groups.get(groupName);
        if (group != null) {
            group.addPart(partName);
            partToGroup.put(partName, groupName);
        }
    }

    /**
     * Retire une partie de son groupe actuel
     */
    public void removePartFromCurrentGroup(String partName) {
        String currentGroup = partToGroup.get(partName);
        if (currentGroup != null) {
            LUTGroup group = groups.get(currentGroup);
            if (group != null) {
                group.removePart(partName);
            }
            partToGroup.remove(partName);
        }
    }

    /**
     * Obtient le groupe d'une partie
     */
    public String getPartGroup(String partName) {
        return partToGroup.get(partName);
    }

    /**
     * Obtient tous les groupes
     */
    public ObservableList<String> getGroupNames() {
        return FXCollections.observableArrayList(groups.keySet());
    }

    /**
     * Obtient un groupe par nom
     */
    public LUTGroup getGroup(String groupName) {
        return groups.get(groupName);
    }

    /**
     * Vérifie si un groupe existe
     */
    public boolean hasGroup(String groupName) {
        return groups.containsKey(groupName);
    }

    /**
     * Applique une LUT à toutes les parties d'un groupe
     */
    public void setGroupLUT(String groupName, String lutPath, LUTManager lutManager) {
        LUTGroup group = groups.get(groupName);
        if (group != null) {
            group.setLutPath(lutPath);

            // Charger la LUT pour toutes les parties du groupe
            for (String part : group.getParts()) {
                lutManager.loadLUT(part, lutPath);
                lutManager.setSelectedColor(part, group.getSelectedVariant());
            }
        }
    }

    /**
     * Change la variante pour tout un groupe
     */
    public void setGroupVariant(String groupName, int variant, LUTManager lutManager) {
        LUTGroup group = groups.get(groupName);
        if (group != null) {
            group.setSelectedVariant(variant);

            // Appliquer à toutes les parties du groupe
            for (String part : group.getParts()) {
                if (lutManager.hasLUT(part)) {
                    lutManager.setSelectedColor(part, variant);
                }
            }
        }
    }
}