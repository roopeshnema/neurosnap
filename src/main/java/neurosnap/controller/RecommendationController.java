package neurosnap.controller;

import java.util.List;
import java.util.Map;

import java.util.Optional;
import java.util.UUID;

import neurosnap.dto.Persona;
import neurosnap.dto.RecommendOptionsResponse;
import neurosnap.dto.RecommendRequest;
import neurosnap.service.RecommendationService;
import neurosnap.util.RefiInputValidator;
import org.apache.coyote.BadRequestException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@RestController
@RequestMapping("/neurosnap/api/v1/refi")
@CrossOrigin(origins = "http://localhost:5173")
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
            @RequestHeader( value = "persona-id", required = false ) final String personaId)
    {


        List<String> errors = refiInputValidator.validate(request, personaId);

        if (!errors.isEmpty()) {
            // Return HTTP 400 with the error list
            /*return ResponseEntity
                    .badRequest()
                    .body(Map.of("errors", errors));
            */
            return ResponseEntity.badRequest().body(Map.of("error", Map.of(
                    "code", "VALIDATION_ERROR",
                    "message", String.join("; ", errors),
                    "requestId", UUID.randomUUID().toString()
            )));
        }

        try
        {
            RecommendOptionsResponse response = recommendationService.getRecommendations( request, personaId );
            return ResponseEntity.ok( response );
        } catch ( BadRequestException e ) {
            return ResponseEntity.badRequest().body( Map.of("error", Map.of(
                    "code", "VALIDATION_ERROR",
                    "message", e.getMessage(),
                    "requestId", UUID.randomUUID().toString()
            )));
        }
        catch ( Exception e )
        {
            throw new RuntimeException( e );
        }
    }

    @PostMapping("/example")
    public ResponseEntity<?> examples(@RequestBody RecommendRequest request,
            @RequestHeader( value = "persona-id", required = false ) final String personaId) throws Exception
    {


        List<String> errors = refiInputValidator.validate(request, personaId);

        if (!errors.isEmpty()) {
            // Return HTTP 400 with the error list
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("errors", errors));
        }

        RecommendOptionsResponse response = recommendationService.getExamples(request, personaId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/personas")
    public ResponseEntity<?> personas() throws Exception
    {
        List<Persona> response = recommendationService.getPersonas();
        return ResponseEntity.ok(response);
    }
}
