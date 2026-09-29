package xp.dev.effect;

import java.util.*;

public final class ConvertColors {
    private static final Map<String, Integer> COLORS;
    static {
        Map<String, Integer> colors = new LinkedHashMap<>();
        add(colors, 0x000000, "black", "negro"); add(colors, 0xffffff, "white", "blanco");
        add(colors, 0x15d94b, "green", "verde"); add(colors, 0xf52222, "red", "rojo");
        add(colors, 0x2366ff, "blue", "azul"); add(colors, 0xffe029, "yellow", "amarillo");
        add(colors, 0xff781c, "orange", "naranja"); add(colors, 0x9433ef, "purple", "morado", "violeta");
        add(colors, 0xff62b8, "pink", "rosa"); add(colors, 0x19e7ef, "cyan", "cian");
        add(colors, 0xee24e8, "magenta"); add(colors, 0x777777, "gray", "grey", "gris");
        add(colors, 0x8b4e28, "brown", "marron", "cafe");
        COLORS = Collections.unmodifiableMap(colors);
    }
    private static void add(Map<String, Integer> map, int rgb, String... names) { for (String name : names) map.put(name, rgb); }
    public static OptionalInt find(String name) {
        Integer color = COLORS.get(name.toLowerCase(Locale.ROOT));
        return color == null ? OptionalInt.empty() : OptionalInt.of(color);
    }
    public static Set<String> names() { return COLORS.keySet(); }
    private ConvertColors() {}
}
