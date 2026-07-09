package io.github.silentdevelopment.headdb.paper.sound;

import org.junit.jupiter.api.Test;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SoundConfigTest {

    @Test
    void parsesBundledDefaultsForEverySoundKey() {
        InputStream input = SoundConfigTest.class.getClassLoader().getResourceAsStream("sounds.yml");
        assertNotNull(input);

        Object loaded = new Yaml().load(input);
        assertTrue(loaded instanceof Map<?, ?>);

        @SuppressWarnings("unchecked")
        SoundConfig config = SoundConfig.fromMap((Map<String, Object>) loaded);
        assertTrue(config.enabled());

        for (SoundKey key : SoundKey.values()) {
            SoundEntry entry = config.entry(key);
            assertTrue(entry.enabled(), key.configKey());
            assertFalse(entry.sound().isBlank(), key.configKey());
        }
    }
}
