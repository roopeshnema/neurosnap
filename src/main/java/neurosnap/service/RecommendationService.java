package neurosnap.service;

import java.util.List;
import java.util.Optional;
import neurosnap.dto.Persona;
import neurosnap.dto.RecommendOption;
import neurosnap.dto.RecommendOptionsResponse;
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

    public RecommendOptionsResponse getRecommendations(RecommendRequest request, String personaId ) {

        List<Persona> personaList = personaReaderService.readPersonasFromExcel( "persona.xlsx" );


      //  return Arrays.asList(option1, option2, option3);
        return populateRecommendOptionsResponse();
    }

    private RecommendOptionsResponse populateRecommendOptionsResponse() {
        RecommendOptionsResponse response = new RecommendOptionsResponse();
        response.setPersonaId("P_EARLY_PAYER");
        response.setModelVersion("v1.0.0");
        RecommendOption option1 = new RecommendOption(
                "REFI1", "LOWER_EMI", 18000, 72, 5.9, 1200, 14400, 500,
                 6, 82, true,
                 "Because you always pay early and have strong credit, you qualify for a lower EMI with minimal risk."
        );
        RecommendOption option2 = new RecommendOption(
                "REFI2", "BALANCED",  20000,  60,  6.5,  0,  0, 0,0,
                 74,  false,
                "Balanced plan keeps your payments stable while slightly reducing total interest."
        );
        RecommendOption option3 = new RecommendOption(
               "REFI3", "FASTER_CLOSURE", 22000,  48,  5.5,  1000,  12000,
                500, 12,  68, false,
                "Higher EMI but lets you close the loan faster and save $50,000 in interest."
        );
        RecommendOption[] optionArray =  new RecommendOption[] { option1, option2, option3 };
        response.setRecommendations(optionArray);
       return response;
    }

}
