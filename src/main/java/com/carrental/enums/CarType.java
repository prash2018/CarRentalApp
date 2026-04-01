package com.carrental.enums;

// The three car types supported by the rental system.
// Demonstrates: ABSTRACTION — clients work with CarType, not concrete classes.
public enum CarType {
    SEDAN,
    SUV,
    VAN;

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }
}
