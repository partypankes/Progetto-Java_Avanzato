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


    // Legge il contenuto del file con il nome passato a riferimento e lo trasforma in stringa eliminando la punteggiatura (dovrebbe valere anche per i testi unicode)
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

    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                '}';
    }
}
