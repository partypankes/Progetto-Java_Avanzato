package gruppo2.service;


import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.*;
import java.util.stream.Collectors;

public class CollectionStatisticsService extends Service<String> {
    private List<Document> documents;
    private Map<Document, Map<String, Integer>> resultMapByDocument;

    public CollectionStatisticsService(List<Document> documents, Map<Document, Map<String, Integer>> resultMapByDocument) {
        this.documents = documents;
        this.resultMapByDocument = resultMapByDocument;
    }

    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                int totalWords = 0;
                int sentenceCount = 0;
                int documentCount = documents.size();
                Map<String, Integer> globalWordCount = new HashMap<>();

                for (Document doc : documents) {
                    String testoDocumento = doc.getDocument_text().replaceAll("'", " ");

                    Map<String, Integer> documentVector = new HashMap<>(resultMapByDocument.get(doc)); // Crea una copia

                    // Aggiorna il conteggio totale delle parole e delle frasi
                    totalWords += testoDocumento.split("\\s").length - 1;
                    sentenceCount += (int) Arrays.stream(testoDocumento.split("[.!?]")).filter(s -> !s.trim().isEmpty()).count();

                    // Aggiorna il conteggio globale delle parole
                    for (Map.Entry<String, Integer> entry : documentVector.entrySet()) {
                        // Divide il titolo in parole utilizzando spazi e punteggiatura come delimitatori
                        String[] parole = doc.getTitle().split("\\W+");
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
                        .collect(Collectors.toList());

                // Creazione del messaggio di statistica
                StringBuilder statsMessage = new StringBuilder();
                statsMessage.append("Numero totale di parole: ").append(totalWords).append("\n");
                statsMessage.append("Numero di frasi: ").append(sentenceCount).append("\n");
                statsMessage.append("Numero di documenti: ").append(documentCount).append("\n");
                statsMessage.append("Le 5 parole più comuni:\n");
                commonWords.forEach(entry -> statsMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

                return statsMessage.toString();
            }
        };
    }
}
