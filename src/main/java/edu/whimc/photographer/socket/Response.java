package edu.whimc.photographer.socket;

import java.util.UUID;

/**
 * Holds observation response information.
 */
public class Response {
    /** The client's UUID. */
    private UUID clientUuid;
    /** The observation's ID. */
    private int observationId;
    /** The observation feedback. */
    private String feedback;
    /** The caption generated from the screenshot. */
    private String generatedCaption;
    /** The observation's score. */
    private float score;

    /** Constructs a Response. */
    public Response() {
        super();
    }

    /**
     * Constructs a Response.
     * @param clientUuid The client's UUID.
     * @param observationId The observation's ID.
     * @param feedback The observation feedback.
     * @param generatedCaption The caption generated from the screenshot.
     * @param score The observation's score.
     */
    public Response(UUID clientUuid, int observationId, String feedback, String generatedCaption, float score) {
        this.clientUuid = clientUuid;
        this.observationId = observationId;
        this.feedback = feedback;
        this.generatedCaption = generatedCaption;
        this.score = score;
    }

    /** @return The client's UUID. */
    public UUID getClientUuid() {
        return clientUuid;
    }

    /** @return The observation ID. */
    public int getObservationId() {
        return observationId;
    }

    /** @return The observation feedback. */
    public String getFeedback() {
        return feedback;
    }

    /** @return The caption generated from the screenshot. */
    public String getGeneratedCaption() {
        return generatedCaption;
    }

    /** The observation's score. */
    public float getScore() {
        return score;
    }
}
