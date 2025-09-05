package neurosnap.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendOptionsResponse
{

    private String personaId;
    private String modelVersion;
    private RecommendOption[] recommendations;


}

