package xp.dev.effect;

import java.util.Locale;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConvertTest {
    @Test void blackIsActuallyZeroAndAliasesMatch() {
        assertEquals(0, ConvertColors.find("black").orElseThrow());
        assertEquals(ConvertColors.find("black"), ConvertColors.find("negro"));
        assertEquals(ConvertColors.find("green"), ConvertColors.find("verde"));
        assertEquals(ConvertColors.find("white"), ConvertColors.find("blanco"));
        assertTrue(ConvertColors.find("does-not-exist").isEmpty());
        for (String name : ConvertColors.names()) {
            int color = ConvertColors.find(name).orElseThrow();
            assertTrue(color >= 0 && color <= 0xffffff);
        }
    }
    @Test void colorParsingDoesNotDependOnLocale() {
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            assertEquals(ConvertColors.find("pink"), ConvertColors.find("PINK"));
        } finally { Locale.setDefault(previous); }
    }
    @Test void convertCanReverseDuringTransition() {
        Animation a = new Animation(); a.target(true, 0, Animation.CONVERT_TICKS);
        assertEquals(.5f, a.at(10, Animation.CONVERT_TICKS));
        a.target(false, 10, Animation.CONVERT_TICKS);
        assertEquals(.5f, a.at(10, Animation.CONVERT_TICKS));
        assertEquals(0, a.at(20, Animation.CONVERT_TICKS));
        a.target(true, 20, Animation.CONVERT_TICKS);
        assertEquals(1, a.at(40, Animation.CONVERT_TICKS));
    }
    @Test void invalidNetworkStateCannotReachRenderer() {
        assertTrue(new ConvertPayload("minecraft:overworld",0,1,true).valid());
        assertFalse(new ConvertPayload("minecraft:overworld",-1,1,true).valid());
        assertFalse(new ConvertPayload("minecraft:overworld",0x1000000,1,true).valid());
        assertFalse(new ConvertPayload("minecraft:overworld",0,Float.NaN,true).valid());
        assertFalse(new ConvertPayload("minecraft:overworld",0,Float.POSITIVE_INFINITY,true).valid());
        assertFalse(new ConvertPayload("minecraft:overworld",0,-.1f,false).valid());
    }
}
