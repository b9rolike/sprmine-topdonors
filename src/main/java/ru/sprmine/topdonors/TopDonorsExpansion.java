package ru.sprmine.topdonors;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TopDonorsExpansion extends PlaceholderExpansion {

    private final TopDonors plugin;

    public TopDonorsExpansion(TopDonors plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "topdonors";
    }

    @Override
    public @NotNull String getAuthor() {
        return "SPRmine";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0.0";
    }

    @Override
    public boolean persist() {
        return true;
    }

    /**
     * Поддерживаемые плейсхолдеры:
     * %topdonors_1_name% ... %topdonors_10_name%
     * %topdonors_1_amount% ... %topdonors_10_amount%
     */
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        List<TopDonors.DonorEntry> top = plugin.getTopDonors();

        String[] parts = params.split("_");
        if (parts.length != 2) {
            return "";
        }

        int position;
        try {
            position = Integer.parseInt(parts[0]);
        } catch (NumberFormatException e) {
            return "";
        }

        String type = parts[1]; // name или amount

        if (position < 1 || position > 10) {
            return "";
        }

        int index = position - 1;

        if (index >= top.size()) {
            // Нет данных для этой позиции
            if (type.equalsIgnoreCase("name")) {
                return "———";
            } else if (type.equalsIgnoreCase("amount")) {
                return "0";
            }
            return "";
        }

        TopDonors.DonorEntry entry = top.get(index);

        if (type.equalsIgnoreCase("name")) {
            return entry.name;
        } else if (type.equalsIgnoreCase("amount")) {
            // Форматируем без лишних десятичных знаков если число целое
            if (entry.amount == Math.floor(entry.amount)) {
                return String.valueOf((long) entry.amount);
            }
            return String.format("%.2f", entry.amount);
        }

        return "";
    }
}
