package dbrighthd.elytratrails.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dbrighthd.elytratrails.ElytraTrails;
import net.fabricmc.loader.api.FabricLoader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class ConfigManager {

    //using GSON instead of codecs so I dont need to make subclasses
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    //I was initially going to just make it getDir/config im glad theres a built in way i may be stupid
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("elytratrails.json");

    private static ModConfig config = new ModConfig();

    public static ModConfig getConfig() {
        return config;
    }
    public static void setConfig(ModConfig newConfig) {
        config = Objects.requireNonNullElseGet(newConfig, ModConfig::new);
        config.validate();
    }

    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            config = new ModConfig();
            config.validate();
            save();
            return;
        }

        try (BufferedReader reader = Files.newBufferedReader(CONFIG_PATH)) {
            ModConfig loaded = GSON.fromJson(reader, ModConfig.class);

            //this *should* set default configs on first launch
            if (loaded == null) {
                loaded = new ModConfig();
            }

            loaded.validate();
            config = loaded;
        } catch (IOException | JsonParseException e) {
            ElytraTrails.LOGGER.warn("Error while parsing elytratrails config: {}", e.getMessage());
            config = new ModConfig();
            config.validate();
            save();
        }
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            config.validate();
            try (BufferedWriter writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(config, writer);
            }
        } catch (IOException e) {
            ElytraTrails.LOGGER.warn("Error while saving elytratrails config: {}", e.getMessage());
        }
    }

    public static void save(ModConfig newConfig) {
        config = newConfig;
        config.validate();
        save();
    }

    //I want to eventually make my own config screen, this will run when "reset to defaults" is clicked, unless I do it a better way
    @SuppressWarnings("unused")
    public static void reset() {
        config = new ModConfig();
        config.validate();
        save();
    }
}
