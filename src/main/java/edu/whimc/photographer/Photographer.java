package edu.whimc.photographer;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import edu.whimc.observations.Observations;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.models.ObserveEvent;
import edu.whimc.photographer.socket.Response;
import java.util.LinkedList;
import java.util.Optional;
import java.util.Queue;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * The main plugin class.
 */
public final class Photographer extends JavaPlugin {
    /** The queue of screenshots to be taken. */
    private final Queue<ObserveEvent> photographs = new LinkedList<>();

    /** The socket server. */
    private SocketIOServer socketServer;
    /** The WHIMC Observations plugin. */
    private Observations observationsPlugin;

    /** {@inheritDoc} */
    @Override
    public void onEnable() {
        super.saveDefaultConfig();

        this.observationsPlugin = (Observations) getServer().getPluginManager().getPlugin("WHIMC-Observations");

        Configuration config = new Configuration();
        config.setHostname(super.getConfig().getString("websocket.host"));
        config.setPort(super.getConfig().getInt("websocket.port"));

        // set up socket server from config settings
        this.socketServer = new SocketIOServer(config);

        // assign UUID to connected clients
        this.socketServer.addConnectListener(client -> {
            UUID uuid = UUID.randomUUID();
            client.set("uuid", uuid);
            client.sendEvent("uuid", uuid);
            this.getLogger().info("connected to " + client.getRemoteAddress() + " [" + uuid + "]");
        });

        // unregister photographer on client disconnect
        this.socketServer.addDisconnectListener(client -> {
                CameraOperator.getCameraOperator(client.get("uuid")).ifPresent(co -> {
                    co.unregister();
                    this.getLogger().info("Disconnected from " + co.getClient().getRemoteAddress() +
                            " [" + co.getClientUuid() + "]");
                });
        });

        // testing event listener
        socketServer.addEventListener("test", String.class, (client, message, ackRequest) ->
                Bukkit.broadcastMessage(client.getRemoteAddress() + " [" + client.get("uuid") + "]: " + message)
        );

        // sends the player a feedback response following their observation
        socketServer.addEventListener("screenshot_response", Response.class, (client, response, ackRequest) -> {
            CameraOperator.getCameraOperator(response.getClientUuid()).ifPresent(co -> co.setCurrentEvent(null));
            Observation observation = Observation.getObservation(response.getObservationId());
            Player player = Bukkit.getPlayer(observation.getPlayer());

            // send more detailed information to console
            this.getLogger().info("Observation ID: " + response.getObservationId());
            this.getLogger().info("Feedback: " + response.getFeedback());
            this.getLogger().info("Generated caption: " + response.getGeneratedCaption());
            this.getLogger().info("Score: " + response.getScore());

            // exit if player not present
            if (player == null) {
                return;
            }

            // send player feedback
            player.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + "Your observation has been analyzed!");
            player.sendMessage(ChatColor.BOLD + "FEEDBACK: " + ChatColor.YELLOW + response.getFeedback());
            player.sendMessage(ChatColor.BOLD + "GENERATED: " + ChatColor.GRAY + ChatColor.ITALIC + response.getGeneratedCaption());
        });

        // re-queue screenshot on fail
        socketServer.addEventListener("screenshot_failed", null, (client, response, ackRequest) -> {
            // Re-add the event to the queue if the screenshot failed
            CameraOperator.getCameraOperator(client.get("uuid")).ifPresent(co -> {
                ObserveEvent event = co.getCurrentEvent();
                co.setCurrentEvent(null);
                this.photographs.add(event);
                this.getLogger().warning("Observation " + event.getObservation().getId() +
                        " could not be screenshotted. Re-adding to queue");
            });
        });

        this.socketServer.start();

        // Queue up some events
        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            if (this.photographs.isEmpty()) {
                return;
            }
            CameraOperator.getAvailableOperator().ifPresent(co -> co.photograph(this.photographs.poll()));
        }, 20, 20);

        getServer().getPluginManager().registerEvents(new Listeners(this), this);

        PluginCommand cmd = getCommand("photographer");
        PhotographerCommand photographerCommand = new PhotographerCommand(this);
        cmd.setExecutor(photographerCommand);
        cmd.setTabCompleter(photographerCommand);
    }

    /** {@inheritDoc} */
    @Override
    public void onDisable() {
        CameraOperator.getAllCameraOperators().forEach(co -> co.unregister());
        this.socketServer.stop();
    }

    /**
     * Adds the Observe event to photograph queue.
     * @param event The Observe event.
     */
    public void queuePhotograph(ObserveEvent event) {
        this.photographs.add(event);
    }

    /** @return The Socket server. */
    public SocketIOServer getSocketServer() {
        return this.socketServer;
    }

    /** @return The instance of the Observations plugin. */
    public Observations getObservationsPlugin() {
        return this.observationsPlugin;
    }

    /**
     * @param clientUuid The UUID of the client.
     * @return The client associated with the given UUID.
     */
    public Optional<SocketIOClient> getClient(UUID clientUuid) {
        return this.socketServer.getAllClients().stream()
                .filter(c -> c.get("uuid").equals(clientUuid))
                .findFirst();
    }

}
