package dev.kesh.storyteller;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public final class Text {
    private static final MiniMessage MM = MiniMessage.miniMessage();

    private Text() {
    }

    public static Component mm(String input) {
        return MM.deserialize(input);
    }

    /** MiniMessage component without the default italic that items get. */
    public static Component item(String input) {
        return mm(input).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE);
    }

    public static String plain(String input) {
        return MM.stripTags(input);
    }

    public static String fill(String input, Player player) {
        return input.replace("%player%", player.getName()).replace("{player}", player.getName());
    }

    /** Word-wraps raw MiniMessage text into lore lines. */
    public static List<Component> wrap(String raw, int width, String basePrefix) {
        List<Component> out = new ArrayList<>();
        for (String paragraph : raw.split("\n", -1)) {
            if (paragraph.isBlank()) {
                out.add(Component.empty());
                continue;
            }
            StringBuilder line = new StringBuilder();
            int length = 0;
            for (String word : paragraph.split(" ")) {
                int wordLength = plain(word).length();
                if (length > 0 && length + 1 + wordLength > width) {
                    out.add(item(basePrefix + line));
                    line.setLength(0);
                    length = 0;
                }
                if (length > 0) {
                    line.append(' ');
                    length++;
                }
                line.append(word);
                length += wordLength;
            }
            if (length > 0) {
                out.add(item(basePrefix + line));
            }
        }
        return out;
    }
}
