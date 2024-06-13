package gruppo2.service;

import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.*;
import java.util.stream.Collectors;


/**
 * DocumentStatisticsService è una classe che estende Service<String> per calcolare e restituire statistiche
 * dettagliate su un documento.
 * <p>
 * Questa classe permette di calcolare statistiche come il numero totale di parole, parole uniche, numero di frasi,
 * le parole più comuni e la percentuale di similarità rispetto a una query.
 */
public class DocumentStatisticsService extends Service<String> {
    private final Document document;
    private final Map<Document, Map<String, Integer>> resultMapByDocument;
    private final Map<Document, Double> corrispondenzaSimiliarita;

    /**
     * Costruttore per inizializzare DocumentStatisticsService con i parametri necessari.
     *
     * @param document                  il documento su cui calcolare le statistiche
     * @param resultMapByDocument       una mappa che contiene i conteggi delle parole per documento
     * @param corrispondenzaSimiliarita una mappa che contiene i valori di similarità per documento
     */
    public DocumentStatisticsService(Document document, Map<Document, Map<String, Integer>> resultMapByDocument, Map<Document, Double> corrispondenzaSimiliarita) {
        this.document = document;
        this.resultMapByDocument = resultMapByDocument;
        this.corrispondenzaSimiliarita = corrispondenzaSimiliarita;
    }

    /**
     * Crea un task per calcolare le statistiche del documento.
     *
     * @return il task per calcolare le statistiche del documento
     */
    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() {
                String testoDocumento = document.document_text().replaceAll("'", " ");
                Map<String, Integer> documentVector = new HashMap<>(resultMapByDocument.get(document)); // Crea una copia

                // Calcolo delle statistiche
                int totalWords = testoDocumento.split("\\s").length - 1;
                int uniqueWords = documentVector.size();
                int sentenceCount = (int) Arrays.stream(testoDocumento.split("[.!?]")).filter(s -> !s.trim().isEmpty()).count();

                // Le 5 parole più comuni
                List<Map.Entry<String, Integer>> commonWords = documentVector.entrySet().stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().trim().isEmpty()) // Filtra chiavi vuote o spazi
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(5)
                        .toList();

                // Rimuovi il conteggio delle parole del titolo dalla copia del vector
                for (Map.Entry<String, Integer> entry : commonWords) {
                    String[] parole = document.title().split("\\W+");
                    int conteggio = 0;

                    for (String p : parole) {
                        if (p.equalsIgnoreCase(entry.getKey())) {
                            conteggio++;
                        }
                    }

                    if (conteggio > 0) {
                        entry.setValue(entry.getValue() - conteggio);
                    }
                }

                StringBuilder statsMessage = new StringBuilder();
                statsMessage.append("\u2022 ").append("Numero di parole: ").append(totalWords).append("\n\n");
                statsMessage.append("\u2022 ").append("Numero di parole \n  uniche: ").append(uniqueWords).append("\n\n");
                statsMessage.append("\u2022 ").append("Numero di frasi: ").append(sentenceCount).append("\n\n");
                if(commonWords.isEmpty()){
                    statsMessage.append("Nessuna parola comune presente").append(uniqueWords).append("\n");
                } else {
                    statsMessage.append("\u2022 ").append("Le 5 parole più comuni: \n");
                    commonWords.forEach(entry -> statsMessage.append("   - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));
                }


                if (corrispondenzaSimiliarita.get(document) == null) {
                    statsMessage.append("\n \u2022 ").append("Percentuale di similaritá rispetto \n  alla query: non definita\n");
                } else {
                    double percentuale = corrispondenzaSimiliarita.get(document) * 100;
                    statsMessage.append("\n \u2022 ").append("Percentuale di similaritá rispetto \n  alla query: ").append(Math.round(percentuale)).append("%\n");
                }

                return statsMessage.toString();
            }
        };
    }
}
