package ui;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class FavoritesManager {

    private static final Path FAVORITES_FILE =
            Paths.get(System.getProperty("user.home"), ".mcsr_favorites.txt");

    private static final Set<String> favorites = new LinkedHashSet<>();
    private static boolean initialized = false;

    private static void ensureLoaded() {
        if (initialized) return;
        initialized = true;

        if (Files.notExists(FAVORITES_FILE)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(FAVORITES_FILE);
            for (String line : lines) {
                String uuid = line.trim();
                if (!uuid.isEmpty()) {
                    favorites.add(uuid);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to read favorites file: " + e.getMessage());
        }
    }

    private static void save() {
        try {
            List<String> lines = new ArrayList<>(favorites);
            Files.write(FAVORITES_FILE, lines,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
        } catch (IOException e) {
            System.err.println("Failed to save favorites file: " + e.getMessage());
        }
    }

    public static boolean isFavorite(String uuid) {
        ensureLoaded();
        return favorites.contains(uuid);
    }

    public static void addFavorite(String uuid) {
        ensureLoaded();
        if (favorites.add(uuid)) {
            save();
        }
    }

    public static void removeFavorite(String uuid) {
        ensureLoaded();
        if (favorites.remove(uuid)) {
            save();
        }
    }

    public static List<String> getFavorites() {
        ensureLoaded();
        return new ArrayList<>(favorites);
    }
}