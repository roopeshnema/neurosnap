package neurosnap.controller;

import java.util.Arrays;
import java.util.List;
import neurosnap.dto.RecommendOptions;
import neurosnap.dto.RecommendRequest;
import neurosnap.service.RecommendationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/neurosnap/api/v1/refi")
public class RecommendationController
{

    private final RecommendationService recommendationService;

    public RecommendationController( RecommendationService recommendationService )
    {
        this.recommendationService = recommendationService;
    }

    @PostMapping("/recommend")
    public List<RecommendOptions> recommendOptions(
            @RequestBody RecommendRequest request
    ) {

        return recommendationService.getRecommendations( request );
    }
}
