package me.nullrush.solstice.commands.impl;

import me.nullrush.solstice.Solstice;
import me.nullrush.solstice.commands.Command;
import me.nullrush.solstice.commands.RegisterCommand;
import me.nullrush.solstice.modules.Module;
import me.nullrush.solstice.utils.chat.ChatUtils;

import java.util.ArrayList;

@RegisterCommand(name = "chatnotify", tag = "ChatNotify", description = "Manages the toggle notification status of the client's modules.", syntax = "<true|false|list|reset> | <[module]> <true|false|reset>")
public class ChatNotifyCommand extends Command {
    @Override
    public void execute(String[] args) {
        if (args.length == 2) {
            Module module = Solstice.MODULE_MANAGER.getModule(args[0]);
            if (module == null) {
                Solstice.CHAT_MANAGER.tagged("Could not find the module specified.", getTag(), getName());
                return;
            }

            switch (args[1]) {
                case "true" -> {
                    module.chatNotify.setValue(true);
                    Solstice.CHAT_MANAGER.tagged("Successfully set the module's notification status to " + ChatUtils.getPrimary() + "true" + ChatUtils.getSecondary() + ".", getTag(), getName());
                }
                case "false" -> {
                    module.chatNotify.setValue(false);
                    Solstice.CHAT_MANAGER.tagged("Successfully set the module's notification status to " + ChatUtils.getPrimary() + "false" + ChatUtils.getSecondary() + ".", getTag(), getName());
                }
                case "reset" -> {
                    module.chatNotify.setValue(module.chatNotify.getDefaultValue());
                    Solstice.CHAT_MANAGER.tagged("Successfully set the module's notification status to it's default value.", getTag(), getName());
                }
                default -> messageSyntax();
            }
        } else if (args.length == 1) {
            switch (args[0]) {
                case "true" -> {
                    for (Module module : Solstice.MODULE_MANAGER.getModules()) module.chatNotify.setValue(true);
                    Solstice.CHAT_MANAGER.tagged("Successfully set every module's notification status to " + ChatUtils.getPrimary() + "true" + ChatUtils.getSecondary() + ".", getTag(), getName());
                }
                case "false" -> {
                    for (Module module : Solstice.MODULE_MANAGER.getModules()) module.chatNotify.setValue(false);
                    Solstice.CHAT_MANAGER.tagged("Successfully set every module's notification status to " + ChatUtils.getPrimary() + "false" + ChatUtils.getSecondary() + ".", getTag(), getName());
                }
                case "list" -> {
                    ArrayList<Module> notifiableModules = new ArrayList<>(Solstice.MODULE_MANAGER.getModules().stream().filter(m -> m.chatNotify.getValue()).toList());

                    if (notifiableModules.isEmpty()) {
                        Solstice.CHAT_MANAGER.tagged("There are currently no modules with their notification status set to " + ChatUtils.getPrimary() + "true" + ChatUtils.getSecondary() + ".", getTag(), getName() + "-list");
                    } else {
                        StringBuilder modulesString = new StringBuilder();
                        int index = 0;

                        for (Module module : notifiableModules) {
                            modulesString.append(ChatUtils.getSecondary()).append(module.getName()).append(index + 1 == notifiableModules.size() ? "" : ", ");
                            index++;
                        }

                        Solstice.CHAT_MANAGER.message(ChatUtils.getSecondary() + "Notifiable Modules " + ChatUtils.getPrimary() + "[" + ChatUtils.getSecondary() + notifiableModules.size() + ChatUtils.getPrimary() + "]: " + ChatUtils.getSecondary() + modulesString, getName() + "-list");
                    }
                }
                case "reset" -> {
                    for (Module module : Solstice.MODULE_MANAGER.getModules()) module.chatNotify.setValue(module.chatNotify.getDefaultValue());
                    Solstice.CHAT_MANAGER.tagged("Successfully set every module's notification status to it's default value.", getTag(), getName());
                }
                default -> messageSyntax();
            }
        } else {
            messageSyntax();
        }
    }
}
