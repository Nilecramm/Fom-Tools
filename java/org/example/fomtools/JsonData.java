package org.example.fomtools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class JsonData {
    public Map<String, ActionWrapper> base_arm_left;
    public Map<String, ActionWrapper> base_arm_right;
    public Map<String, ActionWrapper> base_chest;
    public Map<String, ActionWrapper> base_head;
    public Map<String, ActionWrapper> base_legs;
    public Map<String, ActionWrapper> sleeve_left;
    public Map<String, ActionWrapper> tool;
    public Map<String, ActionWrapper> tool_effect;
    public Map<String, ActionWrapper> sleeve_right;
    public Map<String, ActionWrapper> head_gear;
    public Map<String, ActionWrapper> face_gear;
    public Map<String, ActionWrapper> hair_front;
    public Map<String, ActionWrapper> eyes;
    public Map<String, ActionWrapper> hair_mid;
    public Map<String, ActionWrapper> face;
    public Map<String, ActionWrapper> facial_hair;
    public Map<String, ActionWrapper> waist;
    public Map<String, ActionWrapper> feet;
    public Map<String, ActionWrapper> legs;
    public Map<String, ActionWrapper> torso;
    public Map<String, ActionWrapper> back_gear;
    public Map<String, ActionWrapper> hair_back;
    public Map<String, ActionWrapper> head_gear_back;
    public Map<String, ActionWrapper> base_effect;
}
