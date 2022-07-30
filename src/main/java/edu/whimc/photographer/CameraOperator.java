package edu.whimc.photographer;

import com.corundumstudio.socketio.SocketIOClient;
import com.gmail.filoghost.holographicdisplays.api.HologramsAPI;
import edu.whimc.observations.models.Observation;
import edu.whimc.observations.models.ObserveEvent;
import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

/**
 * Converts a player into a camera.
 */
public class CameraOperator {
    /** The set of active CameraOperators. */
    private static final Set<CameraOperator> operators = new HashSet<>();

    /** The instance of the plugin. */
    private final Photographer plugin;
    /** The player's UUID. */
    private final UUID playerUuid;
    /** The client's UUID. */
    private final UUID clientUuid;

    /** The player's previous game mode. */
    private GameMode prevGameMode;
    /** The player's previous location. */
    private Location prevLocation;

    /** The Observe event. */
    private @Nullable ObserveEvent currentEvent = null;

    /**
     * Constructs a CameraOperator.
     * @param plugin The instance of the plugin.
     * @param player The player.
     * @param client The client.
     */
    private CameraOperator(Photographer plugin, Player player, SocketIOClient client) {
        this.plugin = plugin;
        this.playerUuid = player.getUniqueId();
        this.clientUuid = client.get("uuid");
    }

    /**
     * Registers a player as a CameraOperator.
     * @param plugin The instance of the plugin.
     * @param player The player.
     * @param client The client.
     * @return The newly registered CameraOperator.
     */
    public static Optional<CameraOperator> registerCameraOperator(Photographer plugin, Player player, SocketIOClient client) {
        // check if player and client are present
        if (getCameraOperator(player.getUniqueId()).isPresent()
                || getCameraOperator(client.get("uuid")).isPresent()) {
            return Optional.empty();
        }

        // construct a new CameraOperator
        CameraOperator operator = new CameraOperator(plugin, player, client);

        // Save player state
        operator.prevLocation = player.getLocation();
        operator.prevGameMode = player.getGameMode();

        // Prepare the player for being a camera operator
        player.setGameMode(GameMode.SPECTATOR);

        // Hide all players
        Bukkit.getOnlinePlayers().forEach(p -> player.hidePlayer(plugin, p));

        // Hide all holograms from the player
        HologramsAPI.getHolograms(plugin.getObservationsPlugin()).forEach(hologram ->
                hologram.getVisibilityManager().hideTo(player));

        CameraOperator.operators.add(operator);
        return Optional.of(operator);
    }

    /**
     * Unregister the CameraOperator and return them to their previous player state.
     */
    public void unregister() {
        Player player = getPlayer();
        player.sendMessage(ChatColor.GOLD + "" + ChatColor.GOLD + "You are no longer a photographer");
        CameraOperator.operators.remove(this);

        Bukkit.getScheduler().runTask(this.plugin, () -> {
            // Restore the state of the player
            player.teleport(this.prevLocation);
            player.setGameMode(this.prevGameMode);

            // Show all players
            Bukkit.getOnlinePlayers().forEach(p -> player.showPlayer(this.plugin, p));

            // Show all holograms
            HologramsAPI.getHolograms(plugin.getObservationsPlugin()).forEach(hologram ->
                    hologram.getVisibilityManager().resetVisibility(player));
        });
    }

    /** @return The first available CameraOperator. */
    public static Optional<CameraOperator> getAvailableOperator() {
        return CameraOperator.operators.stream()
                .filter(CameraOperator::isAvailable)
                .findFirst();
    }

    /** @return The collection of all active CameraOperators. */
    public static Collection<CameraOperator> getAllCameraOperators() {
        return CameraOperator.operators;
    }

    /**
     * @param playerOrSocketUuid the UUID of the player or client.
     * @return The CameraOperator associated with the passed UUID.
     */
    public static Optional<CameraOperator> getCameraOperator(UUID playerOrSocketUuid) {
        for (CameraOperator operator : CameraOperator.operators) {
            if (operator.playerUuid.equals(playerOrSocketUuid)
                    || operator.clientUuid.equals(playerOrSocketUuid)) {
                return Optional.of(operator);
            }
        }
        return Optional.empty();
    }

    /**
     * Clears the CameraOperator's screen to prepare for a screenshot.
     * @param event The Observe event.
     */
    public void photograph(ObserveEvent event) {
        this.currentEvent = event;
        Player player = getPlayer();
        player.sendMessage(ChatColor.YELLOW + event.getPlayer().getName() + " made an observation. " +
                "Teleporting to view location for photograph.");

        Observation observation = event.getObservation();

        // Make the observation invisible
        observation.getHologram().getVisibilityManager().hideTo(player);
        player.teleport(observation.getViewLocation());

        String strippedObservation = ChatColor.stripColor(observation.getObservation());

        Bukkit.getScheduler().runTaskLaterAsynchronously(this.plugin, () ->
                getClient().sendEvent("screenshot", observation.getId(), strippedObservation)
        , 20);
    }

    /** @return The player associated with this CameraOperator. */
    public Player getPlayer() {
        return Bukkit.getPlayer(this.playerUuid);
    }

    /** @return If the CameraOperator is available to photograph. */
    public boolean isAvailable() {
        return this.currentEvent == null;
    }

    /** @return The UUID of the player. */
    public UUID getPlayerUuid() {
        return this.playerUuid;
    }

    /** @return The UUID of the client. */
    public UUID getClientUuid() {
        return this.clientUuid;
    }

    /** @return The current Observe event. */
    public @Nullable ObserveEvent getCurrentEvent() {
        return this.currentEvent;
    }

    /**
     * Sets the current Observe event to the passed event.
     * @param event The Observe event.
     */
    public void setCurrentEvent(@Nullable ObserveEvent event) {
        this.currentEvent = event;
    }

    /** @return The client associated with this CameraOperator. */
    public SocketIOClient getClient() {
        // This should never be null
        return this.plugin.getClient(this.clientUuid).get();
    }

}
