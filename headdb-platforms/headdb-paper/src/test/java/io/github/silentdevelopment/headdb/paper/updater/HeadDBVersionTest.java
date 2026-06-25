package io.github.silentdevelopment.headdb.paper.updater;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HeadDBVersionTest {

    @Test
    void releaseVersionBeatsPrerelease() {
        HeadDBVersion release = HeadDBVersion.parse("7.0.0");
        HeadDBVersion candidate = HeadDBVersion.parse("7.0.0-rc.2");

        assertTrue(release.compareTo(candidate) > 0);
        assertEquals(UpdateKind.VERSION, release.updateKindComparedTo(candidate, true));
    }

    @Test
    void buildMetadataCanBeTreatedAsUpdate() {
        HeadDBVersion current = HeadDBVersion.parse("7.0.0-rc.2+build.4");
        HeadDBVersion candidate = HeadDBVersion.parse("v7.0.0-rc.2+build.5");

        assertEquals(UpdateKind.BUILD, candidate.updateKindComparedTo(current, true));
        assertEquals(UpdateKind.NONE, candidate.updateKindComparedTo(current, false));
    }

    @Test
    void candidateWithoutBuildBeatsCurrentBuildOfSameBaseVersion() {
        HeadDBVersion current = HeadDBVersion.parse("7.0.0-rc.2+build.5");
        HeadDBVersion candidate = HeadDBVersion.parse("7.0.0-rc.2");

        assertEquals(UpdateKind.VERSION, candidate.updateKindComparedTo(current, true));
    }

    @Test
    void olderVersionIsNotUpdate() {
        HeadDBVersion current = HeadDBVersion.parse("7.0.0");
        HeadDBVersion candidate = HeadDBVersion.parse("7.0.0-rc.9");

        assertEquals(UpdateKind.NONE, candidate.updateKindComparedTo(current, true));
    }

}
