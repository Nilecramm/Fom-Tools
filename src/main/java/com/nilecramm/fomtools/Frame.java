package com.nilecramm.fomtools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Frame {
    public int[] offset;
    public double duration;
    public int target_frame;
    public int depth;
    public Integer broadcast_message; // Peut être null, donc `Integer`

    public String toString() {
        return "offset x: " + offset[0] + "offset y: " + offset[1] + ", duration: " + duration + ", target_frame: " + target_frame + ", depth: " + depth + ", broadcast_message: " + broadcast_message;
    }
}


