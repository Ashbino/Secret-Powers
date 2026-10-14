package com.thegildedking.secretpowers.util;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Logger;

public final class YamlStore {

    private YamlStore() {}

    /** Writes to a temp file first, then replaces the real file, so a crash cannot corrupt it. */
    public static void save(YamlConfiguration yaml, File file, Logger log) {
        try {
            Path target = file.toPath();
            Files.createDirectories(target.getParent());
            Path tmp = target.resolveSibling(file.getName() + ".tmp");
            Files.writeString(tmp, yaml.saveToString(), StandardCharsets.UTF_8);
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.severe("Could not save " + file.getName() + ": " + e.getMessage());
        }
    }
}
