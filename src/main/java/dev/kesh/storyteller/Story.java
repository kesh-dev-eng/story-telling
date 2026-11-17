package dev.kesh.storyteller;

import java.util.List;

public record Story(String id, String title, List<String> pages, List<String> guide, List<String> rewards) {
}
