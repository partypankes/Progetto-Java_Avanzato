package gruppo2.service;

import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.*;
import java.util.stream.Collectors;

public class DocumentStatisticsService extends Service<String> {
    private Document document;
    private Map<Document, Map<String, Integer>> resultMapByDocument;
    private Map<Document, Double> corrispondenzaSimiliarita;

    public DocumentStatisticsService(Document document, Map<Document, Map<String, Integer>> resultMapByDocument, Map<Document, Double> corrispondenzaSimiliarita) {
        this.document = document;
        this.resultMapByDocument = resultMapByDocument;
        this.corrispondenzaSimiliarita = corrispondenzaSimiliarita;
    }

    @Override
    protected Task<String> createTask() {
        return new Task<>() {
            @Override
            protected String call() throws Exception {
                String testoDocumento = document.getDocument_text().replaceAll("'", " ");
                Map<String, Integer> documentVector = new HashMap<>(resultMapByDocument.get(document)); // Crea una copia

                // Calcolo delle statistiche
                int totalWords = testoDocumento.split("\\s").length - 1;
                int uniqueWords = documentVector.size();
                int sentenceCount = (int) Arrays.stream(testoDocumento.split("[.!?]")).filter(s -> !s.trim().isEmpty()).count();

                // Le 5 parole più comuni
                List<Map.Entry<String, Integer>> commonWords = documentVector.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(5)
                        .collect(Collectors.toList());

                // Rimuovi il conteggio delle parole del titolo dalla copia del vector
                for (Map.Entry<String, Integer> entry : commonWords) {
                    String[] parole = document.getTitle().split("\\W+");
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
                statsMessage.append("Numero totale di parole: ").append(totalWords).append("\n");
                statsMessage.append("Numero di parole uniche: ").append(uniqueWords).append("\n");
                statsMessage.append("Numero di frasi: ").append(sentenceCount).append("\n");
                statsMessage.append("Le 5 parole più comuni sono:\n");
                commonWords.forEach(entry -> statsMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

                if (corrispondenzaSimiliarita.get(document) == null) {
                    statsMessage.append("Percentuale di similaritá rispetto alla \nquery: non definita\n");
                } else {
                    double percentuale = corrispondenzaSimiliarita.get(document) * 100;
                    statsMessage.append("Percentuale di similaritá rispetto alla query: \n").append(Math.round(percentuale)).append("%\n");
                }

                return statsMessage.toString();
            }
        };
    }
}
