package neurosnap.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import neurosnap.dto.Persona;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

@Service
public class PersonaReaderService
{

    public List<Persona> readPersonasFromExcel( String filePath )
    {
        List<Persona> personas = new ArrayList<>();

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
                persona.setIncome( row.getCell( 2 ).getNumericCellValue() );
                persona.setPaymentBehavior( row.getCell( 3 ).getStringCellValue() );
                persona.setRefiExperience( row.getCell( 4 ).getStringCellValue() );
                persona.setCreditScore( ( int ) row.getCell( 5 ).getNumericCellValue() );
                persona.setExistingLoanAmount( row.getCell( 6 ).getNumericCellValue() );
                persona.setExistingInterestRate( row.getCell( 7 ).getNumericCellValue() );
                persona.setExistingPendingAmount( row.getCell( 8 ).getNumericCellValue() );
                persona.setPaymentHistory( row.getCell( 9 ).getStringCellValue() );

                personas.add( persona );
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
        }

        return personas;
    }
}
