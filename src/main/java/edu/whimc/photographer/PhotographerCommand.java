package edu.whimc.photographer;

import com.corundumstudio.socketio.SocketIOClient;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

/**
 * The base command class. Handles all /photographer subcommands.
 */
public class PhotographerCommand implements CommandExecutor, TabCompleter {
    /** The instance of the plugin. */
    private final Photographer plugin;

    /**
     * Constructs a PhotographerCommand.
     * @param plugin the instance of the plugin.
     */
    public PhotographerCommand(Photographer plugin) {
        this.plugin = plugin;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // exit if sender does not have sufficient permission level
        if (!sender.hasPermission("whimc-photographer.admin")) {
            sender.sendMessage(ChatColor.RED + "No permission!");
            return true;
        }

        // exit if no subcommand provided
        if (args.length == 0) {
            sender.sendMessage("/photographer clients");
            sender.sendMessage("/photographer disconnect-all");
            sender.sendMessage("/photographer collect <uuid>");
            sender.sendMessage("/photographer stop-collecting");
            sender.sendMessage("/photographer disconnect <uuid>");
            sender.sendMessage("/photographer send <uuid> <msg>");
            return true;
        }

        // get subcommand name
        String subCmd = args[0];

        // clients subcommand - lists currently active clients (UUID, IP, current player)
        if (subCmd.equalsIgnoreCase("clients")) {
            sender.sendMessage("Clients:");
            for (SocketIOClient client : this.plugin.getSocketServer().getAllClients()) {
                sender.sendMessage("> " + ChatColor.AQUA + client.get("uuid"));
                sender.sendMessage("|  IP: " + ChatColor.AQUA + client.getRemoteAddress());
                CameraOperator.getCameraOperator(client.get("uuid")).ifPresentOrElse(
                        co -> sender.sendMessage("|  Player: " + ChatColor.AQUA + co.getPlayer().getName()),
                        () -> sender.sendMessage("|  Player: " + ChatColor.DARK_GRAY + "N/A"));
            }
            return true;
        }

        // disconnect-all subcommand - disconnects all currently connected clients
        if (subCmd.equalsIgnoreCase("disconnect-all")) {
            this.plugin.getSocketServer().getAllClients().forEach(client -> {
                sender.sendMessage("Disconnecting " + client.getRemoteAddress() + ": " + client.get("uuid"));
                CameraOperator.getCameraOperator(client.get("uuid")).ifPresent(CameraOperator::unregister);
                client.disconnect();
            });
            return true;
        }

        // stop-collecting subcommand - un-registers player as photographer
        if (subCmd.equalsIgnoreCase("stop-collecting")) {
            // exit if command not issued by player
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "Must be a player!");
                return true;
            }
            CameraOperator.getCameraOperator(((Player) sender).getUniqueId()).ifPresentOrElse(
                    co -> co.unregister(),
                    () -> sender.sendMessage("You are not a photographer!"));
            return true;
        }

        // exit if no matching single-argument subcommand
        if (args.length < 2) {
            return true;
        }

        // check for valid UUID and get associated client
        UUID uuid;
        try {
            uuid = UUID.fromString(args[1]);
        } catch (IllegalArgumentException exc) {
            sender.sendMessage("Invalid UUID");
            return true;
        }
        Optional<SocketIOClient> clientOpt = this.plugin.getClient(uuid);

        // exit if client cannot be found
        if (!clientOpt.isPresent()) {
            sender.sendMessage("Client not found");
            return true;
        }

        SocketIOClient client = clientOpt.get();

        // disconnect subcommand - disconnects client with provided UUID
        if (subCmd.equalsIgnoreCase("disconnect")) {
            sender.sendMessage("Disconnecting " + client.getRemoteAddress() + ": " + client.get("uuid"));
            CameraOperator.getCameraOperator(client.get("uuid")).ifPresent(CameraOperator::unregister);
            client.disconnect();
            return true;
        }

        // collect subcommand - become a photographer
        if (subCmd.equalsIgnoreCase("collect")) {
            // exit if command not issued by player
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "You have to be a player");
            }
            Player player = (Player) sender;

            // make current player a photographer
            Optional<CameraOperator> camera = CameraOperator.registerCameraOperator(this.plugin, player, client);
            if (camera.isPresent()) {
                player.sendMessage(ChatColor.GREEN + "You have become a photographer for " + client.get("uuid"));
            } else {
                player.sendMessage(ChatColor.RED + "You are already collecting or that client is in use");
            }

            return true;
        }

        // send subcommand - sends a message to the client with provided UUID
        if (subCmd.equalsIgnoreCase("send")) {
            // exit if not enough arguments
            if (args.length <= 2) {
                sender.sendMessage(ChatColor.ITALIC + "/photographer send <uuid> <msg>");
                return true;
            }

            // send message to client
            String message = StringUtils.join(args, " ", 2, args.length);
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () ->
                    client.sendEvent("message", message)
            );
            return true;
        }

        return true;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length <= 1) {
            return Arrays.asList("clients", "disconnect-all", "disconnect", "collect", "send", "stop-collecting").stream()
                    .filter(arg -> arg.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }
        return this.plugin.getSocketServer().getAllClients().stream()
                .map(client-> client.get("uuid").toString())
                .collect(Collectors.toList());
    }

}
