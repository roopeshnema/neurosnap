package neurosnap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * RecommendOptionsResponse represents the complete response
 * returned by the Refinance Recommendation API.
 *
 * Key principles:
 * - Always return 3 recommendations (LOWER_EMI, BALANCED, FASTER_CLOSURE).
 * - Include metadata (personaId, modelVersion, requestId, demoMode)
 *   to make the system traceable and demo-friendly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendOptionsResponse {

    /** The persona ID used for generating recommendations (e.g., P_PLANNER). */
    private String personaId;

    /** Version of the recommendation model or engine logic (e.g., "v1.0.0"). */
    private String modelVersion;

    /** Unique request ID (UUID) for traceability across logs, backend, and UI. */
    private String requestId;

    /** Flag to indicate demo mode (true = mock/demo, false = production logic). */
    private boolean demoMode;

    /** Array of refinance plan recommendations (should always contain 3). */
    private RecommendOption[] recommendations;

}