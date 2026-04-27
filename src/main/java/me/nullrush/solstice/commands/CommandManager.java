package me.nullrush.solstice.commands;

import lombok.Getter;
import lombok.Setter;
import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.commands.impl.ModuleCommand;
import me.nullrush.solstice.events.SubscribeEvent;
import me.nullrush.solstice.events.impl.ChatInputEvent;
import org.reflections.Reflections;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;

@Getter @Setter
public class CommandManager {
    private final ArrayList<Command> commands = new ArrayList<>();
    private String prefix = ".";

    public CommandManager() {
        Solstice.EVENT_HANDLER.subscribe(this);

        try {
            for (Class<?> clazz : new Reflections("me.nullrush.solstice.commands.impl").getSubTypesOf(Command.class)) {
                if (clazz.getAnnotation(RegisterCommand.class) == null) continue;
                if (clazz == ModuleCommand.class) continue;

                commands.add((Command) clazz.getDeclaredConstructor().newInstance());
            }
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException exception) {
            Solstice.LOGGER.error("Failed to register the client's modules!", exception);
        }

        Solstice.MODULE_MANAGER.getModules().forEach(m -> commands.add(new ModuleCommand(m)));
    }

    @SubscribeEvent
    public void onChatInput(ChatInputEvent event) {
        String message = event.getMessage();
        if (!message.startsWith(prefix)) return;

        event.setCancelled(true);
        execute(event.getMessage());
    }

    public void execute(String input) {
        String[] split = input.substring(prefix.length()).split(" ");
        boolean foundCommand = false;

        if (split.length > 0) {
            for (Command command : commands) {
                if (!command.getName().equalsIgnoreCase(split[0]) && !command.getAliases().contains(split[0].toLowerCase())) continue;
                command.execute(Arrays.copyOfRange(split, 1, split.length));
                foundCommand = true;
            }
        }

        if (!foundCommand) Solstice.CHAT_MANAGER.warn("Could not find the command specified.");
    }

    public Command getCommand(String name) {
        return commands.stream().filter(c -> c.getName().equalsIgnoreCase(name.toLowerCase()) || c.getAliases().contains(name.toLowerCase())).findFirst().orElse(null);
    }
}
