package com.nonxedy.nonchat.util.chat.packets;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public final class DisplayEntityUtil {
    private static final String TEXT_DISPLAY_CLASS = "org.bukkit.entity.TextDisplay";
    private static final String ENTITY_CLASS = "org.bukkit.entity.Entity";
    private static final String BILLBOARD_CLASS = "org.bukkit.entity.Display$Billboard";

    private DisplayEntityUtil() {
    }

    public static List<Object> spawnMultilineBubble(
        Player player,
        String message,
        Location location,
        double scale,
        double scaleX,
        double scaleY,
        double scaleZ,
        Color backgroundColor
    ) {
        if (!isSupported()) {
            return List.of();
        }

        List<Object> displays = new ArrayList<>();
        String[] lines = message.split("\\R");

        for (int i = 0; i < lines.length; i++) {
            Location lineLocation = location.clone().add(0.0D, (lines.length - i - 1) * 0.25D, 0.0D);
            Object display = spawnDisplay(player.getWorld(), lineLocation);
            if (display == null) {
                continue;
            }

            invoke(display, "text", new Class<?>[] { Component.class }, Component.text(lines[i]));
            invoke(display, "setPersistent", new Class<?>[] { boolean.class }, false);
            invoke(display, "setInvulnerable", new Class<?>[] { boolean.class }, true);
            invoke(display, "setShadowed", new Class<?>[] { boolean.class }, false);
            invoke(display, "setSeeThrough", new Class<?>[] { boolean.class }, false);

            if (backgroundColor != null) {
                invoke(display, "setBackgroundColor", new Class<?>[] { Color.class }, backgroundColor);
            }

            setBillboard(display);
            displays.add(display);
        }

        return displays;
    }

    public static void updateBubblesLocation(Collection<?> bubbles, Location location) {
        if (bubbles == null || bubbles.isEmpty()) {
            return;
        }

        int total = bubbles.size();
        int index = 0;
        for (Object bubble : bubbles) {
            Location target = location.clone().add(0.0D, (total - index - 1) * 0.25D, 0.0D);
            teleport(bubble, target);
            index++;
        }
    }

    public static void removeBubbles(Collection<?> bubbles) {
        if (bubbles == null || bubbles.isEmpty()) {
            return;
        }

        for (Object bubble : bubbles) {
            invoke(bubble, "remove", new Class<?>[0]);
        }
    }

    public static boolean isSupported() {
        try {
            Class.forName(TEXT_DISPLAY_CLASS);
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    private static Object spawnDisplay(World world, Location location) {
        try {
            Class<?> textDisplayClass = Class.forName(TEXT_DISPLAY_CLASS);
            Method spawn = World.class.getMethod("spawn", Location.class, Class.class);
            return spawn.invoke(world, location, textDisplayClass);
        } catch (ReflectiveOperationException ignored) {
            return null;
        }
    }

    private static void teleport(Object entity, Location location) {
        try {
            Class<?> entityClass = Class.forName(ENTITY_CLASS);
            Method teleport = entityClass.getMethod("teleport", Location.class);
            teleport.invoke(entity, location);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private static void setBillboard(Object display) {
        try {
            Class<?> billboardClass = Class.forName(BILLBOARD_CLASS);
            Object center = Enum.valueOf((Class<? extends Enum>) billboardClass.asSubclass(Enum.class), "CENTER");
            invoke(display, "setBillboard", new Class<?>[] { billboardClass }, center);
        } catch (ReflectiveOperationException ignored) {
        }
    }

    private static void invoke(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        if (target == null) {
            return;
        }

        try {
            Method method = target.getClass().getMethod(methodName, parameterTypes);
            method.invoke(target, args);
        } catch (ReflectiveOperationException ignored) {
        }
    }
}
