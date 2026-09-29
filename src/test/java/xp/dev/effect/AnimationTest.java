package xp.dev.effect;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AnimationTest {
    @Test void fracturesBeforeOpeningAndReversesInOrder() {
        Animation animation = new Animation(); animation.target(true, 0, Animation.RIFT_TICKS);
        assertEquals(0, Animation.opening(animation.at(59, Animation.RIFT_TICKS)));
        assertEquals(1, Animation.fracture(animation.at(60, Animation.RIFT_TICKS)), .0001);
        assertEquals(0, Animation.opening(animation.at(60, Animation.RIFT_TICKS)), .0001);
        assertEquals(1, animation.at(110, Animation.RIFT_TICKS));
        animation.target(false, 110, Animation.RIFT_TICKS);
        assertEquals(1, Animation.fracture(animation.at(140, Animation.RIFT_TICKS)), .0001);
        assertTrue(Animation.opening(animation.at(140, Animation.RIFT_TICKS)) > 0);
        assertEquals(0, Animation.opening(animation.at(161, Animation.RIFT_TICKS)));
        assertEquals(0, animation.at(220, Animation.RIFT_TICKS));
    }
    @Test void interruptAndResumeNeverJump() {
        Animation a = new Animation(); a.target(true, 10, Animation.RIFT_TICKS);
        float before = a.at(45, Animation.RIFT_TICKS); a.target(false, 45, Animation.RIFT_TICKS);
        assertEquals(before, a.at(45, Animation.RIFT_TICKS));
        float reverse = a.at(60, Animation.RIFT_TICKS); a.target(true, 60, Animation.RIFT_TICKS);
        assertEquals(reverse, a.at(60, Animation.RIFT_TICKS));
        assertEquals(1, a.at(1000, Animation.RIFT_TICKS));
        a.target(false, 1000, Animation.RIFT_TICKS); assertEquals(0, a.at(2000, Animation.RIFT_TICKS));
    }
    @Test void skyTakesEightSeconds() {
        Animation a = new Animation(); a.target(true, 20, Animation.SKY_TICKS);
        assertEquals(.5, a.at(100, Animation.SKY_TICKS)); assertEquals(1, a.at(180, Animation.SKY_TICKS));
        a.target(false, 180, Animation.SKY_TICKS); assertEquals(.5, a.at(260, Animation.SKY_TICKS));
    }
}
