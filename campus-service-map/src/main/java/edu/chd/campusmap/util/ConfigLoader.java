package edu.chd.campusmap.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {
    private static final String CONFIG_FILE = "/config/database.properties";
    private static final Properties properties = new Properties();

    static {
        try (InputStream is = ConfigLoader.class.getResourceAsStream(CONFIG_FILE)) {
            if (is == null) {
                throw new RuntimeException("配置文件未找到: " + CONFIG_FILE);
            }
            properties.load(is);
        } catch (IOException e) {
            throw new RuntimeException("加载配置文件失败", e);
        }
    }

    public static String get(String key) {
        return properties.getProperty(key);
    }
}
