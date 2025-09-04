package neurosnap.service;

import java.util.Arrays;
import java.util.List;
import neurosnap.dto.Persona;
import neurosnap.dto.RecommendOptions;
import neurosnap.dto.RecommendRequest;
import org.springframework.stereotype.Service;

@Service
public class RecommendationService
{
    private final PersonaReaderService personaReaderService;

    public RecommendationService( PersonaReaderService personaReaderService )
    {
        this.personaReaderService = personaReaderService;
    }

    public List<RecommendOptions> getRecommendations( RecommendRequest request ) {
        RecommendOptions option1 = new RecommendOptions(String.valueOf(request.getLoanAmount()), 3.25, 15);
        RecommendOptions option2 = new RecommendOptions("Lender B", 3.75, 30);
        RecommendOptions option3 = new RecommendOptions("Lender C", 4.10, 20);

        List<Persona> personaList = personaReaderService.readPersonasFromExcel( "persona.xlsx" );

        return Arrays.asList(option1, option2, option3);
    }

}
