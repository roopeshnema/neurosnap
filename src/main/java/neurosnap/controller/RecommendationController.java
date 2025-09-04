package neurosnap.controller;

import java.util.Arrays;
import java.util.List;
import neurosnap.dto.RecommendOptions;
import neurosnap.dto.RecommendRequest;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/neurosnap")
public class RecommendationController
{
    @PostMapping("/recommend")
    public List<RecommendOptions> recommendOptions(
            @RequestBody RecommendRequest request
    ) {

        RecommendOptions option1 = new RecommendOptions(String.valueOf(request.getLoanAmount()), 3.25, 15);
        RecommendOptions option2 = new RecommendOptions("Lender B", 3.75, 30);
        RecommendOptions option3 = new RecommendOptions("Lender C", 4.10, 20);

        return Arrays.asList(option1, option2, option3);
    }
}
