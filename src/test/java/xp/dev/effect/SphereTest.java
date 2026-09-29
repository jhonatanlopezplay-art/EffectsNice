package xp.dev.effect;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SphereTest {
    @Test void incomingAndOutgoingProjectilesHitTheShell() {
        assertEquals(.5, SphereGeometry.contact(12,0,0,-4,0,0,10,0), 1e-9);
        assertEquals(.5, SphereGeometry.contact(8,0,0,4,0,0,10,0), 1e-9);
        assertEquals(.5, SphereGeometry.contact(0,-12,0,0,4,0,10,0), 1e-9);
    }
    @Test void fastProjectileCannotTunnelThroughBothSides() {
        assertEquals(.125, SphereGeometry.contact(-20,0,0,80,0,0,10,0), 1e-9);
    }
    @Test void projectilesThatDoNotTouchTheShellAreUnaffected() {
        assertTrue(Double.isNaN(SphereGeometry.contact(0,0,0,1,2,3,10,.2)));
        assertTrue(Double.isNaN(SphereGeometry.contact(12,0,0,4,0,0,10,.2)));
        assertTrue(Double.isNaN(SphereGeometry.contact(-20,12,0,80,0,0,10,.2)));
        assertTrue(Double.isNaN(SphereGeometry.contact(0,0,0,0,0,0,10,.2)));
    }
    @Test void projectileWidthAndGrazingHitsAreCovered() {
        assertEquals(0, SphereGeometry.contact(10.1,0,0,0,0,0,10,.2));
        assertEquals(.4, SphereGeometry.contact(11,0,0,-2,0,0,10,.2), 1e-9);
        assertEquals(.5, SphereGeometry.contact(-10,10,0,20,0,0,10,0), 1e-9);
    }
    @Test void everyMeshIsClosedAndMostlyHexagonal() {
        for (int level = 1; level <= 4; level++) {
            var cells = SphereMesh.create(level);
            assertEquals(10*(1 << (2*level))+2, cells.size());
            assertEquals(12, cells.stream().filter(c -> c.corners().size()==5).count());
            Map<Set<SphereMesh.Point>, Integer> edges = new HashMap<>();
            for (var cell : cells) {
                assertTrue(cell.corners().size()==5 || cell.corners().size()==6);
                assertEquals(1, cell.center().length(), 1e-9);
                for (int i=0; i<cell.corners().size(); i++) {
                    var a = cell.corners().get(i);
                    var b = cell.corners().get((i+1)%cell.corners().size());
                    assertEquals(1, a.length(), 1e-9);
                    assertTrue(a.subtract(cell.center()).cross(b.subtract(cell.center())).dot(cell.center()) > 0);
                    edges.merge(Set.of(a,b), 1, Integer::sum);
                }
            }
            assertTrue(edges.values().stream().allMatch(count -> count==2), "Every panel edge must have exactly two neighbours");
        }
    }
    @Test void invalidSphereNetworkValuesAreRejected() {
        assertTrue(payload(32, 1, 0).valid());
        assertFalse(payload(Float.NaN, 1, 0).valid());
        assertFalse(payload(129, 1, 0).valid());
        assertFalse(payload(1, 1, 0).valid());
        assertFalse(payload(32, Float.POSITIVE_INFINITY, 0).valid());
        assertFalse(payload(32, -.1f, 0).valid());
        assertFalse(payload(32, 1, Double.NaN).valid());
        assertFalse(payload(32, 1, 30_000_001).valid());
    }
    @Test void appearanceCanReverseWithoutJumping() {
        Animation a = new Animation();
        a.target(true, 0, SphereGeometry.FADE_TICKS);
        a.target(false, 8, SphereGeometry.FADE_TICKS);
        assertEquals(.4f, a.at(8, SphereGeometry.FADE_TICKS), 1e-6);
        assertEquals(0, a.at(16, SphereGeometry.FADE_TICKS));
    }
    private SpherePayload payload(float radius, float progress, double x) {
        return new SpherePayload("minecraft:overworld",x,64,0,radius,progress,true);
    }
}
