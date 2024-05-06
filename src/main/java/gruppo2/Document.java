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

    public Document(String title) { // l'ho inizializzato per fare la prova
        this.title = title;
        document_text = null;
    }

    public String getTitle() {
        return title;
    }

    public String getDocument_text() {
        return document_text;
    }


    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                '}';
    }
}
