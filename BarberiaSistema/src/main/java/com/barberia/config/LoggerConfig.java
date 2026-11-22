package com.barberia.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggerConfig {

    private LoggerConfig() {
    }

    public static Logger getLogger(Class<?> clase) {
        return LoggerFactory.getLogger(clase);
    }
}