package gruppo2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Document {
    private final String title;


    public Document(String title) {
        this.title = title;

    }

    public String getTitle() {
        return title;
    }



    public static String leggiContenuto ( String nomeFile ){
        StringBuilder result = new StringBuilder();
        try(BufferedReader bfr = new BufferedReader(new FileReader( nomeFile ))){
            String line = bfr.readLine();
            while ((line = bfr.readLine()) != null) {
               result.append(line).append("\n");
                System.out.println(result);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                '}';
    }
}
