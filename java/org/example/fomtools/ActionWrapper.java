package org.example.fomtools;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ActionWrapper {
    public Frame[] east;
    public Frame[] north;
    public Frame[] south;
}