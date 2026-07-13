package io.github.silentdevelopment.headdb.paper.command;

import io.github.silentdevelopment.headdb.paper.HeadDBPlugin;
import io.github.silentdevelopment.relay.paper.PaperCommandManager;
import io.github.silentdevelopment.relay.paper.bootstrap.PaperLifecycleCommandRegistrar;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class Commands {

    private final HeadDBPlugin plugin;

    public Commands(@NotNull HeadDBPlugin plugin) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
    }

    public void register() {
        PaperCommandManager manager = new PaperCommandManager(builder -> builder
                .responseRenderer(new CommandResponseRenderer(plugin))
                .responseFunction((receiver, message) -> plugin.messages().send(receiver, message))
        );
        PaperLifecycleCommandRegistrar registrar = new PaperLifecycleCommandRegistrar(plugin, manager);

        registrar.register(new RootCommand(plugin));
        registrar.install();
    }

}