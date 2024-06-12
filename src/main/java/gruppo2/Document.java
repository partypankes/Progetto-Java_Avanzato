package gruppo2;

import java.io.Serializable;


/**
 * Rappresenta un documento con un nome file, un titolo e un testo del documento.
 */
public record Document(String filename, String title, String document_text) implements Comparable<Document>, Serializable {

    /**
     * Confronta questo documento con un altro documento.
     *
     * @param other L'altro documento da confrontare.
     * @return un valore negativo, zero o un valore positivo se questo documento
     * è rispettivamente minore, uguale o maggiore dell'altro documento.
     */
    @Override
    public int compareTo(Document other) {
        int titleComparison = this.title.compareTo(other.title());
        if (titleComparison != 0) {
            return titleComparison;
        } else {
            return this.document_text.compareTo(other.document_text());
        }
    }
}
