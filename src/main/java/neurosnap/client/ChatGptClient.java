package neurosnap.client;

import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired })
public class ChatGptClient
{
    private static final String API_KEY = "";
    private static final String API_URL = "https://api.openai.com/v1/chat/completions";
    private final OkHttpClient client = new OkHttpClient();

    public String sendPrompt(String prompt) throws IOException {

        JSONObject json = new JSONObject();
        json.put("model", "gpt-4o"); // Use the appropriate model
        json.put("messages", new JSONObject[]{
                new JSONObject()
                        .put("role", "user")
                        .put("content", prompt)
        });
        json.put("max_tokens",550); // Adjust as needed


        // Create request body
        RequestBody body = RequestBody.create(
                MediaType.parse("application/json"),
                json.toString()
        );

        Request request = new Request.Builder()
                .url(API_URL)
                .post(body)
                .addHeader("Authorization", "Bearer " + API_KEY)
                .addHeader("Content-Type", "application/json")
                .build();

        System.out.println("chat gpt request : " + request.toString());

        try {
            Response response = client.newCall( request ).execute();
            if (!response.isSuccessful()) {
                    System.out.println("Something went wrong 1");
            }
                String finalResp = response.body().string();
                System.out.println("RESPONSE BODY START : " + finalResp);
                System.out.println("RESPONSE BODY END : " );

            JSONObject jsonResponse = new JSONObject( finalResp );
            String responseContent = jsonResponse
                    .getJSONArray( "choices" )
                    .getJSONObject( 0 )
                    .getJSONObject( "message" )
                    .getString( "content" );


            System.out.println( "Response: " + responseContent );
            return finalResp;
        } catch ( Exception e ) {
            System.out.println("Something went wrong");
        }

        return prompt;
    }
}
