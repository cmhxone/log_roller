package com.comtec.log_roller.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConfigurationPropertiesTest {
    
    @Test
    public void configurationPropertiesTest() {
        ConfigurationProperties properties = ConfigurationProperties.getInstance();

        assertEquals("test", properties.getString("test.string"));
        assertEquals(2023, properties.getInt("test.int"));
        assertEquals(true, properties.getBoolean("test.boolean"));
    }
}
