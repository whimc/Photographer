package edu.whimc.photographer;

import edu.whimc.observations.models.ObserveEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Holds all the plugin's event listeners.
 */
public class Listeners implements Listener {
    /** The instance of the plugin. */
    private final Photographer plugin;

    /**
     * Constructs a Listeners object.
     * @param plugin The instance of the plugin.
     */
    public Listeners(Photographer plugin) {
        this.plugin = plugin;
    }

    /**
     * Queues up a CameraOperator when a player makes an Observation.
     * @param event The Observation event.
     */
    @EventHandler
    public void onObservation(ObserveEvent event) {
        // check if any CameraOperators are registered
        if (!CameraOperator.getAllCameraOperators().isEmpty()) {
            this.plugin.queuePhotograph(event);
        }

        // Wait a few ticks for the hologram to spawn
        Bukkit.getScheduler().runTaskLater(this.plugin, () -> {
           CameraOperator.getAllCameraOperators().forEach(co ->
                   event.getObservation().getHologram().getVisibilityManager().hideTo(co.getPlayer()));
        }, 10);
    }

    /**
     * Unregisters a CameraOperator when the associated player disconnects.
     * @param event The PlayerQuitEvent.
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        CameraOperator.getCameraOperator(player.getUniqueId()).ifPresent(CameraOperator::unregister);
        CameraOperator.getAllCameraOperators().forEach(co -> co.getPlayer().showPlayer(this.plugin, player));
    }
}
