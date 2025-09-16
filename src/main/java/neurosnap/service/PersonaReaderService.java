package neurosnap.service;

import jakarta.annotation.PostConstruct;
import java.util.Optional;
import neurosnap.dto.Persona;
import org.apache.coyote.BadRequestException;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

@Service
public class PersonaReaderService
{

    private List<Persona> personas = new ArrayList<>();
    @PostConstruct
    public void readPersonasFromExcel( )
    {

        String filePath = "persona.xlsx";
        try (InputStream is = PersonaReaderService.class.getClassLoader().getResourceAsStream(filePath);
             Workbook workbook = WorkbookFactory.create(is))
        {

            if (is == null) {
                throw new RuntimeException("File not found in resources: " + filePath);
            }

            Sheet sheet = workbook.getSheetAt( 0 );
            Iterator<Row> rowIterator = sheet.iterator();

            // Skip header
            if ( rowIterator.hasNext() )
            {
                rowIterator.next();
            }

            while ( rowIterator.hasNext() )
            {
                Row row = rowIterator.next();
                Persona persona = new Persona();

                persona.setPersonaId( row.getCell( 0 ).getStringCellValue() );
                persona.setPersonaName( row.getCell( 1 ).getStringCellValue() );
                persona.setIncome( row.getCell( 2 ).getStringCellValue() );
                persona.setPaymentBehavior( row.getCell( 3 ).getStringCellValue() );
                persona.setRefiExperience( row.getCell( 4 ).getStringCellValue() );
                persona.setCreditScore( ( int ) row.getCell( 5 ).getNumericCellValue() );
                persona.setExistingLoanAmount( row.getCell( 6 ).getNumericCellValue() );
                persona.setExistingInterestRate( row.getCell( 7 ).getNumericCellValue() );
                persona.setExistingPendingAmount( row.getCell( 8 ).getNumericCellValue() );
                persona.setPaymentHistory( row.getCell( 9 ).getStringCellValue() );
                persona.setDob(setDateOfBirth(row.getCell( 10 )));
                persona.setSsn( (int) row.getCell( 11 ).getNumericCellValue() );
                persona.setMobileNumber((long)row.getCell( 12 ).getNumericCellValue());
                persona.setVerificationCode((int)row.getCell( 13 ).getNumericCellValue());
                persona.setExistingTenure((int)row.getCell( 14 ).getNumericCellValue());
                persona.setExistingEmi( row.getCell( 15 ).getNumericCellValue());
                persona.setBankName(row.getCell( 16 ).getStringCellValue());
                persona.setCardNumber((long) row.getCell( 17 ).getNumericCellValue());
                persona.setMinimumRefinanceAmt( row.getCell( 18 ).getNumericCellValue());
                persona.setIncomeAmt( row.getCell( 19 ).getNumericCellValue());

                personas.add( persona );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

    }

    private String setDateOfBirth(Cell cell) {
        Date dob = cell.getDateCellValue();
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        return sdf.format(dob);
    }

    public List<Persona> getAllPersona() {
        return personas;
    }

    public Optional<Persona> getPersona (String personaId) throws BadRequestException
    {
        return  Optional.ofNullable( personas.stream()
                .filter( p -> p.getPersonaId().equals( personaId ) )
                .findFirst()
                .orElseThrow( () -> new BadRequestException( "Unknown persona-id: " + personaId ) ) );
    }
}
