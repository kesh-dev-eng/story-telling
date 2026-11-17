package dev.serverguide;

import java.util.List;

public record Step(List<String> text, String sound, String title, String location, List<String> commands) {
}
