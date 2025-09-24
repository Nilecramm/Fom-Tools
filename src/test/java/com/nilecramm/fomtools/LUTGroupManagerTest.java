package com.nilecramm.fomtools;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Simple test to verify LUTGroupManager refresh functionality
 */
public class LUTGroupManagerTest {
    
    private LUTGroupManager groupManager;
    private LUTManager lutManager;
    
    @BeforeEach
    public void setUp() {
        groupManager = new LUTGroupManager();
        lutManager = new LUTManager();
    }
    
    @Test
    public void testRefreshGroupLUTWithNullGroup() {
        // Test that refresh with non-existent group doesn't crash
        assertDoesNotThrow(() -> {
            groupManager.refreshGroupLUT("nonexistent", lutManager);
        });
    }
    
    @Test
    public void testRefreshGroupLUTWithNoPath() {
        // Create a group without LUT path
        groupManager.createGroup("testGroup");
        
        // Test that refresh with group that has no LUT path doesn't crash
        assertDoesNotThrow(() -> {
            groupManager.refreshGroupLUT("testGroup", lutManager);
        });
    }
    
    @Test
    public void testRefreshGroupLUTMethodExists() {
        // Verify the refresh method exists and can be called
        assertTrue(groupManager.getClass().getDeclaredMethods().length > 0);
        
        // Look for the refreshGroupLUT method
        boolean hasRefreshMethod = false;
        for (var method : groupManager.getClass().getDeclaredMethods()) {
            if ("refreshGroupLUT".equals(method.getName())) {
                hasRefreshMethod = true;
                assertEquals(2, method.getParameterCount()); // String groupName, LUTManager lutManager
                break;
            }
        }
        assertTrue(hasRefreshMethod, "refreshGroupLUT method should exist");
    }
    
    @Test
    public void testCreateAndRefreshGroup() {
        String groupName = "testGroup";
        String partName = "testPart";
        
        // Create group and add part
        groupManager.createGroup(groupName);
        groupManager.addPartToGroup(partName, groupName);
        
        // Verify group was created
        assertTrue(groupManager.hasGroup(groupName));
        assertNotNull(groupManager.getGroup(groupName));
        assertTrue(groupManager.getGroup(groupName).hasPart(partName));
        
        // Test refresh (should not crash even without actual LUT file)
        assertDoesNotThrow(() -> {
            groupManager.refreshGroupLUT(groupName, lutManager);
        });
    }
}