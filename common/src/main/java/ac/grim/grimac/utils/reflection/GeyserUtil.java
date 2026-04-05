package ac.grim.grimac.utils.reflection;

import lombok.experimental.UtilityClass;
import org.geysermc.api.Geyser;

import java.lang.reflect.Method;
import java.util.UUID;

@UtilityClass
public class GeyserUtil {
    // Floodgate is the authentication system for Geyser on servers that use Geyser as a proxy instead of installing it as a plugin directly on the server
    private static final Class<?> floodgateApiClass = ReflectionUtils.getClass("org.geysermc.floodgate.api.FloodgateApi");
    private static final Method floodgateGetInstance = floodgateApiClass == null
            ? null
            : ReflectionUtils.getMethod(floodgateApiClass, "getInstance");
    private static final Method floodgateIsFloodgatePlayer = floodgateApiClass == null
            ? null
            : ReflectionUtils.getMethod(floodgateApiClass, "isFloodgatePlayer", UUID.class);
    private static final boolean geyser = ReflectionUtils.hasClass("org.geysermc.api.Geyser");

    public static boolean isBedrockPlayer(UUID uuid) {
        return isFloodgatePlayer(uuid) || (geyser && Geyser.api().isBedrockPlayer(uuid));
    }

    private static boolean isFloodgatePlayer(UUID uuid) {
        if (floodgateGetInstance == null || floodgateIsFloodgatePlayer == null) {
            return false;
        }

        try {
            Object floodgateApi = floodgateGetInstance.invoke(null);
            if (floodgateApi == null) {
                return false;
            }
            Object result = floodgateIsFloodgatePlayer.invoke(floodgateApi, uuid);
            return result instanceof Boolean && (Boolean) result;
        } catch (ReflectiveOperationException ignored) {
            return false;
        }
    }
}
