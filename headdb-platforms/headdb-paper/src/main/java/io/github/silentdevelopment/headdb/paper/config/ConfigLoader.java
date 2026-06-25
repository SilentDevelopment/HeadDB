package io.github.silentdevelopment.headdb.paper.config;

import io.github.silentdevelopment.atlas.bind.BoundConfig;
import io.github.silentdevelopment.atlas.bind.ConfigBinder;
import io.github.silentdevelopment.atlas.bind.ConfigBindings;
import io.github.silentdevelopment.atlas.codec.yaml.YamlConfigCodec;
import io.github.silentdevelopment.atlas.core.ConfigLoaders;
import io.github.silentdevelopment.atlas.core.document.ConfigDocuments;
import io.github.silentdevelopment.atlas.document.MutableCommentedConfigDocument;
import io.github.silentdevelopment.atlas.io.PathConfigResource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ConfigLoader {

    private final Path dataDirectory;

    public ConfigLoader(@NotNull Path dataDirectory) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
    }

    public @NotNull PluginConfig load() {
        Path configPath = dataDirectory.resolve("config.yml");

        try {
            Files.createDirectories(dataDirectory);

            PathConfigResource resource = PathConfigResource.of(configPath);
            YamlConfigCodec codec = YamlConfigCodec.create();

            if (!resource.exists()) {
                generateDefaultConfig(resource, codec);
            } else {
                appendMissingDefaults(configPath);
            }

            BoundConfig<PluginConfig> boundConfig = ConfigBindings.config()
                    .resource(resource)
                    .codec(codec)
                    .bind(PluginConfig.class)
                    .load();

            PluginConfig config = boundConfig.value();
            config.validate();

            return config;
        } catch (Exception exception) {
            throw new ConfigException("Failed to load HeadDB config from " + configPath, exception);
        }
    }

    private void generateDefaultConfig(@NotNull PathConfigResource resource, @NotNull YamlConfigCodec codec) throws Exception {
        saveConfig(resource, codec, new PluginConfig());
    }

    private void saveConfig(@NotNull PathConfigResource resource, @NotNull YamlConfigCodec codec, @NotNull PluginConfig config) throws Exception {
        MutableCommentedConfigDocument document = ConfigDocuments.commented();

        ConfigBinder.create().save(config, document);

        io.github.silentdevelopment.atlas.ConfigLoader loader = ConfigLoaders.builder()
                .resource(resource)
                .codec(codec)
                .build();

        loader.save(document);
    }

    private void appendMissingDefaults(@NotNull Path configPath) throws IOException {
        String content = Files.readString(configPath, StandardCharsets.UTF_8);
        String merged = ConfigDefaultsMerger.mergeMissingDefaults(content);

        if (merged.equals(content)) {
            return;
        }

        Files.writeString(configPath, merged, StandardCharsets.UTF_8);
    }
}
