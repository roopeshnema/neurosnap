package neurosnap.controller;

import java.util.List;
import java.util.Map;

import java.util.Optional;
import neurosnap.dto.RecommendOptionsResponse;
import neurosnap.dto.RecommendRequest;
import neurosnap.service.RecommendationService;
import neurosnap.util.RefiInputValidator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@RequestMapping("/neurosnap/api/v1/refi")
public class RecommendationController
{

    private final RecommendationService recommendationService;
    private final  RefiInputValidator refiInputValidator;

    public RecommendationController(RecommendationService recommendationService, RefiInputValidator refiInputValidator)
    {
        this.recommendationService = recommendationService;
        this.refiInputValidator = refiInputValidator;
    }

    @PostMapping("/recommend")
    public ResponseEntity<?> recommendOptions(@RequestBody RecommendRequest request,
            @RequestHeader( value = "persona-id", required = false ) final String personaId) throws Exception
    {


        List<String> errors = refiInputValidator.validate(request, personaId);

        if (!errors.isEmpty()) {
            // Return HTTP 400 with the error list
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("errors", errors));
        }

        RecommendOptionsResponse response = recommendationService.getRecommendations(request, personaId);
        return ResponseEntity.ok(response);
    }
}
