package com.github.thesilentpro.headdb.core.command;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.thesilentpro.headdb.core.HeadDB;
import com.github.thesilentpro.headdb.core.command.sub.HDBCommandGive;
import com.github.thesilentpro.headdb.core.command.sub.HDBCommandInfo;
import com.github.thesilentpro.headdb.core.command.sub.HDBCommandOpen;
import com.github.thesilentpro.headdb.core.command.sub.HDBCommandSearch;

public class HDBSubCommandManager {

    private final int totalCommandEntries = 1 + 1; // realCommandCount + totalAliasCount

    private final Map<String, HDBSubCommand> commands = new HashMap<>(
            (int)Math.ceil(totalCommandEntries / 0.75f),
            0.75f
    );
    private final List<String> realNames = new ArrayList<>(
            (int)Math.ceil(1 / 0.75f) // realCommandCount
    );

    private final HeadDB plugin;

    public HDBSubCommandManager(HeadDB plugin) {
        this.plugin = plugin;
    }

    public void registerDefaults() {
        register(new HDBCommandInfo(plugin));
        register(new HDBCommandGive(plugin));
        register(new HDBCommandSearch(plugin));
        register(new HDBCommandOpen(plugin));
    }

    public void register(HDBSubCommand command) {
        String name = command.getName();
        this.commands.put(name, command);
        this.realNames.add(name);
        
        String[] aliases = command.getAliases();
        for (String alias : aliases) {
            this.commands.put(alias, command);
        }
    }

    public HDBSubCommand get(String name) {
        return this.commands.get(name);
    }

    public Map<String, HDBSubCommand> getCommands() {
        return commands;
    }

    public List<String> getRealNames() {
        return realNames;
    }

}
