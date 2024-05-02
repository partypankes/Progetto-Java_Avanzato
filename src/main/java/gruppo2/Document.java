package gruppo2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Document {
    private final String title;
    private final String document_text;


    public Document(String title, String documentText) {
        this.title = title;

        document_text = documentText;
    }

    public String getTitle() {
        return title;
    }

    public String getDocument_text() {
        return document_text;
    }

    /* attualmente spostato nel controller, non lo cancello ancora per ora
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


        return result.toString().replaceAll("[^\\s\\p{L}0-9]", "");
    }


     */
    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                '}';
    }
}
