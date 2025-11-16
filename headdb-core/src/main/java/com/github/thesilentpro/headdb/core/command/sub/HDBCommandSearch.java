package com.github.thesilentpro.headdb.core.command.sub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.github.thesilentpro.headdb.api.model.Head;
import com.github.thesilentpro.headdb.core.HeadDB;
import com.github.thesilentpro.headdb.core.command.HDBSubCommand;
import com.github.thesilentpro.headdb.core.menu.gui.HeadsGUI;
import com.github.thesilentpro.headdb.core.util.Compatibility;

import net.kyori.adventure.text.Component;

public class HDBCommandSearch extends HDBSubCommand {

    private final HeadDB plugin;
    private final List<String> completions = List.of("tags:", "category:", "ids:", "--any");

    public HDBCommandSearch(HeadDB plugin) {
        super("search", "Search for specific heads.", "[tags:|category:|ids:] [head]", "Search the database with possible filters.", "s", "find");
        this.plugin = plugin;
    }

    @Override
    public void handle(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            this.plugin.getLocalization().sendMessage(sender, "noConsole");
            return;
        }

        this.plugin.getLocalization().sendMessage(sender, "command.search.start");
        CompletableFuture.supplyAsync(() -> {
            // detect & strip --any
            // Enables loose search (match if any filter passes instead of all).
            boolean any = false;
            List<String> parts = new ArrayList<>(args.length - 1);
            for (int i = 1; i < args.length; i++) {
                if (args[i].equalsIgnoreCase("--any")) {
                    any = true;
                } else {
                    parts.add(args[i]);
                }
            }
            final boolean finalAny = any;

            // parse filters
            String category = null;
            List<String> tags = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            List<String> nameParts = new ArrayList<>();

            for (String token : parts) {
                String lower = token.toLowerCase(Locale.ROOT);
                if (lower.startsWith("category:")) {
                    category = token.substring("category:".length());
                } else if (lower.startsWith("tags:")) {
                    String raw = token.substring("tags:".length());
                    if (!raw.isEmpty()) tags.addAll(Arrays.asList(raw.split(",")));
                } else if (lower.startsWith("ids:")) {
                    String raw = token.substring("ids:".length());
                    if (!raw.isEmpty()) {
                        for (String part : raw.split(",")) {
                            String trimmed = part.trim();
                            if (!trimmed.isEmpty()) {
                                try {
                                    ids.add(Integer.parseInt(trimmed));
                                } catch (NumberFormatException e) {
                                    sender.sendMessage("§cInvalid ID: §f" + trimmed);
                                    return null;
                                }
                            }
                        }
                    }
                } else {
                    nameParts.add(token);
                }
            }
            String nameQuery = String.join(" ", nameParts);

            // Echo filters to player
            String finalCategory = category;
            Compatibility.getMainThreadExecutor(plugin).execute(() -> {
                plugin.getLocalization().sendMessage(sender, "command.search.filter", msg -> msg.replaceText(builder ->
                                builder.matchLiteral("{name}").replacement(!nameQuery.isEmpty() ? nameQuery : "/"))
                                .replaceText(builder -> builder.matchLiteral("{category}").replacement(finalCategory != null ? finalCategory : "/"))
                                .replaceText(builder -> builder.matchLiteral("{tags}").replacement(!tags.isEmpty() ? String.join(",", tags) : "/"))
                                .replaceText(builder -> builder.matchLiteral("{ids}").replacement(!ids.isEmpty() ? String.join(",", ids.stream().map(String::valueOf).toArray(String[]::new)) : "/"))
                                .replaceText(builder -> builder.matchLiteral("{mode}").replacement(finalAny ? "ANY" : "ALL"))
                );
            });

            // lower all your query bits once
            String qCat = category == null ? null : category.toLowerCase(Locale.ROOT);
            String qName = nameQuery.trim().toLowerCase(Locale.ROOT);
            Set<String> tagSet = new HashSet<>(tags.size());
            for (String tag : tags) {
                tagSet.add(tag.toLowerCase(Locale.ROOT));
            }
            Set<Integer> idSet = new HashSet<>(ids);

            List<Head> allHeads = plugin.getHeadApi().getHeads().join();
            List<Head> result   = new ArrayList<>();

            if (finalAny) {
                // ANY‑mode: match if _one_ of the filters hits
                for (Head h : allHeads) {
                    // grab once per head
                    String headCat  = h.getCategory().toLowerCase(Locale.ROOT);
                    String headName = h.getName().toLowerCase(Locale.ROOT);
                    List<String> headTags = h.getTags(); // assume a few tags only

                    boolean matchCat = (headCat.equals(qCat));
                    boolean matchTag = (!tagSet.isEmpty() && headTags.stream().anyMatch(t -> tagSet.contains(t.toLowerCase(Locale.ROOT))));
                    boolean matchId = (!idSet.isEmpty() && idSet.contains(h.getId()));
                    boolean matchName = (!qName.isEmpty() && headName.contains(qName));

                    if (matchCat || matchTag || matchId || matchName) {
                        result.add(h);
                    }
                }
            } else {
                // ALL‑mode: only add if _every_ non‑empty filter passes
                for (Head h : allHeads) {
                    // category
                    if (qCat != null &&
                            !h.getCategory().equalsIgnoreCase(qCat)) {
                        continue;
                    }
                    // tags
                    if (!tagSet.isEmpty()) {
                        // must contain all query tags
                        List<String> headTags = h.getTags();
                        boolean allTagsMatch = true;
                        for (String tq : tagSet) {
                            boolean found = false;
                            for (String ht : headTags) {
                                if (ht.equalsIgnoreCase(tq)) {
                                    found = true;
                                    break;
                                }
                            }
                            if (!found) {
                                allTagsMatch = false;
                                break;
                            }
                        }
                        if (!allTagsMatch) continue;
                    }
                    // ids
                    if (!idSet.isEmpty() && !idSet.contains(h.getId())) {
                        continue;
                    }
                    // name
                    if (!qName.isEmpty() &&
                            !h.getName().toLowerCase(Locale.ROOT).contains(qName)) {
                        continue;
                    }

                    // made it through all checks
                    result.add(h);
                }
            }

            return new SearchResult(result, qName);
        }).thenAcceptAsync(searchResult -> {
            List<Head> heads = searchResult.heads;
            if (heads == null || heads.isEmpty()) {
                this.plugin.getLocalization().sendMessage(player, "command.search.none");
                return;
            }

            plugin.getLocalization().sendMessage(player, "command.search.found", msg -> msg.replaceText(builder -> builder.matchLiteral("{amount}").replacement(String.valueOf(heads.size()))).replaceText(builder -> builder.matchLiteral("{name}").replacement(searchResult.name)));
            new HeadsGUI(plugin, "search_" + player.getUniqueId().toString(), plugin.getLocalization().getMessage(player.getUniqueId(), "menu.search.name").orElseGet(() -> Component.text("HeadDB » Search » " + searchResult.name)).replaceText(builder -> builder.matchLiteral("{name}").replacement(searchResult.name)), heads).open(player);
            Compatibility.playSound(player, plugin.getSoundConfig().get("menu.open"));
        }, Compatibility.getMainThreadExecutor(plugin));
    }

    @Override
    public List<String> handleCompletions(CommandSender sender, String[] args) {
        return completions;
    }

    private record SearchResult(List<Head> heads, String name) {}

}