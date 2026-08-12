package com.example.consumer.service;

/** Pre-existing debt for this project's own rule: a service writing to stdout. */
public class NoisyService {

    public void announce(String message) {
        System.out.println(message);
    }
}
