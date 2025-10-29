package com.nilecramm.fomtools;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.*;

/**
 * Manages groups of body parts that share the same LUT.
 * Allows applying color variants to multiple parts simultaneously.
 */
public class LUTGroupManager {
    private final Map<String, LUTGroup> groups = new HashMap<>();
    private final Map<String, String> partToGroup = new HashMap<>();

    /**
     * Represents a group of body parts sharing the same LUT
     */
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

        // Getters and setters
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
     * Creates a new group
     * @param groupName the name of the group to create
     */
    public void createGroup(String groupName) {
        if (!groups.containsKey(groupName)) {
            groups.put(groupName, new LUTGroup(groupName));
        }
    }

    /**
     * Deletes a group and removes all its parts
     * @param groupName the name of the group to delete
     */
    public void deleteGroup(String groupName) {
        LUTGroup group = groups.get(groupName);
        if (group != null) {
            for (String part : group.getParts()) {
                partToGroup.remove(part);
            }
            groups.remove(groupName);
        }
    }

    /**
     * Adds a body part to a group
     * @param partName the name of the body part
     * @param groupName the name of the group
     */
    public void addPartToGroup(String partName, String groupName) {
        removePartFromCurrentGroup(partName);

        LUTGroup group = groups.get(groupName);
        if (group != null) {
            group.addPart(partName);
            partToGroup.put(partName, groupName);
        }
    }

    /**
     * Removes a body part from its current group
     * @param partName the name of the body part
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
     * Gets the group name for a body part
     * @param partName the name of the body part
     * @return the group name, or null if the part is not in any group
     */
    public String getPartGroup(String partName) {
        return partToGroup.get(partName);
    }

    /**
     * Gets all group names
     * @return observable list of group names
     */
    public ObservableList<String> getGroupNames() {
        return FXCollections.observableArrayList(groups.keySet());
    }

    /**
     * Gets a group by name
     * @param groupName the name of the group
     * @return the LUT group, or null if not found
     */
    public LUTGroup getGroup(String groupName) {
        return groups.get(groupName);
    }

    /**
     * Checks if a group exists
     * @param groupName the name of the group
     * @return true if the group exists
     */
    public boolean hasGroup(String groupName) {
        return groups.containsKey(groupName);
    }

    /**
     * Applies a LUT to all parts in a group
     * @param groupName the name of the group
     * @param lutPath the path to the LUT file
     * @param lutManager the LUT manager instance
     */
    public void setGroupLUT(String groupName, String lutPath, LUTManager lutManager) {
        LUTGroup group = groups.get(groupName);
        if (group != null) {
            group.setLutPath(lutPath);

            for (String part : group.getParts()) {
                lutManager.loadLUT(part, lutPath);
                lutManager.setSelectedColor(part, group.getSelectedVariant());
            }
        }
    }

    /**
     * Changes the color variant for all parts in a group
     * @param groupName the name of the group
     * @param variant the variant index
     * @param lutManager the LUT manager instance
     */
    public void setGroupVariant(String groupName, int variant, LUTManager lutManager) {
        LUTGroup group = groups.get(groupName);
        if (group != null) {
            group.setSelectedVariant(variant);

            for (String part : group.getParts()) {
                if (lutManager.hasLUT(part)) {
                    lutManager.setSelectedColor(part, variant);
                }
            }
        }
    }

    /**
     * Reloads the LUT for a group from disk
     * @param groupName the name of the group
     * @param lutManager the LUT manager instance
     */
    public void refreshGroupLUT(String groupName, LUTManager lutManager) {
        LUTGroup group = groups.get(groupName);
        if (group != null && group.getLutPath() != null) {
            String lutPath = group.getLutPath();
            
            for (String part : group.getParts()) {
                lutManager.loadLUT(part, lutPath);
                lutManager.setSelectedColor(part, group.getSelectedVariant());
            }
        }
    }
}