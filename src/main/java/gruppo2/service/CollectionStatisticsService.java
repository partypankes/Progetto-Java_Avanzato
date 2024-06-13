package gruppo2.service;

import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.*;

/**
 * La classe CollectionStatisticsService estende Service<String> e fornisce un servizio per calcolare le statistiche
 * di una collezione di documenti. Le statistiche includono il numero totale di parole, il numero di frasi,
 * il numero di documenti e le 5 parole più comuni nell'intera collezione.
 */
public class CollectionStatisticsService extends Service<String> {
    private final List<Document> documents;
    private final Map<Document, Map<String, Integer>> resultMapByDocument;

    /**
     * Costruttore della classe CollectionStatisticsService.
     *
     * @param documents              La lista di documenti da analizzare.
     * @param resultMapByDocument    Una mappa che associa ogni documento alla sua mappa di conteggio delle parole.
     */
    public CollectionStatisticsService(List<Document> documents, Map<Document, Map<String, Integer>> resultMapByDocument) {
        this.documents = documents;
        this.resultMapByDocument = resultMapByDocument;
    }

    /**
     * Metodo che crea un task per il calcolo delle statistiche della collezione.
     *
     * @return Un Task che, quando eseguito, calcola le statistiche della collezione di documenti.
     */
    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() {
                int totalWords = 0;
                int sentenceCount = 0;
                int documentCount = documents.size();
                Map<String, Integer> globalWordCount = new HashMap<>();

                for (Document doc : documents) {
                    String testoDocumento = doc.document_text().replaceAll("'", " ");

                    Map<String, Integer> documentVector = new HashMap<>(resultMapByDocument.get(doc)); // Crea una copia

                    // Aggiorna il conteggio totale delle parole e delle frasi
                    totalWords += testoDocumento.split("\\s").length - 1;
                    sentenceCount += (int) Arrays.stream(testoDocumento.split("[.!?]")).filter(s -> !s.trim().isEmpty()).count();

                    // Aggiorna il conteggio globale delle parole
                    for (Map.Entry<String, Integer> entry : documentVector.entrySet()) {
                        // Divide il titolo in parole utilizzando spazi e punteggiatura come delimitatori
                        String[] parole = doc.title().split("\\W+");
                        int conteggio = 0;

                        // Itera attraverso le parole e conta le occorrenze
                        for (String p : parole) {
                            if (p.equalsIgnoreCase(entry.getKey())) {
                                conteggio++;
                            }
                        }

                        // Se il conteggio è maggiore di zero, aggiorna il valore dell'entry
                        if (conteggio > 0) {
                            entry.setValue(entry.getValue() - conteggio);
                        }

                        globalWordCount.put(entry.getKey(), globalWordCount.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }
                }

                // Le 5 parole più comuni nell'intera collezione
                List<Map.Entry<String, Integer>> commonWords = globalWordCount.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(5)
                        .toList();

                // Creazione del messaggio di statistica
                StringBuilder statsMessage = new StringBuilder();
                statsMessage.append("\u2022 ").append("Numero di parole: ").append(totalWords).append("\n\n");
                statsMessage.append("\u2022 ").append("Numero di frasi: ").append(sentenceCount).append("\n\n");
                statsMessage.append("\u2022 ").append("Numero di documenti: ").append(documentCount).append("\n\n");
                statsMessage.append("\u2022 ").append("Le 5 parole più comuni:\n");
                commonWords.forEach(entry -> statsMessage.append("   -  ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

                return statsMessage.toString();
            }
        };
    }
}
