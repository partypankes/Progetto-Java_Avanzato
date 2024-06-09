/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package gruppo2;


import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javafx.collections.FXCollections;
import javafx.collections.ObservableArray;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

/**
 *
 * @author gruppo_02
 */
public class FXMLDocumentController implements Initializable {
    
    @FXML
    private AnchorPane pane1;
    
    @FXML
    private AnchorPane pane2;

    @FXML
    private AnchorPane paneDocumento;

    @FXML
    private TextField queryTf;

    @FXML
    private TextArea corpoDocumento;

    @FXML
    private TextArea statisticheDocumento;

    @FXML
    private Button chiudiDocumento;
    
    @FXML
    private TableView<Document> tableView;

    @FXML
    private TableColumn<Document, String> titleColumn;
    
    private static TreeMap<String, Integer> vocabolario = new TreeMap<>();
    
    private static Map<Document, Double> corrispondenzaSimiliarita = new TreeMap<>(Collections.reverseOrder());

    private static Map<Document, Map<String, Integer>> resultMapByDocument = new HashMap<>();

    private List<String> stopwords;
   
    
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        try {
            loadStopwords();  // Carica le stopwords subito
        } catch (IOException e) {
            e.printStackTrace();
        }

        paneDocumento.setVisible(false);
        selezionaDocumento();

        corpoDocumento.setEditable(false); //il corpo del documento selezionato non può essere modificato

    }



    @FXML
    private void handleQuery() {
        String queryText = queryTf.getText();
        if (queryText == null || queryText.trim().isEmpty()) {
            // mostra tutti i documenti senza filtro quando cancello la query e clicco invio (quindi il text field è vuoto)
            List<Document> allDocuments = resultMapByDocument.keySet().stream().toList();
            tableView.setItems(FXCollections.observableArrayList(allDocuments));
        } else {

            Map<String, Integer> queryVector = textToVector(removeStopwords(queryText), false);
            corrispondenzaSimiliarita.clear();
            for (Map.Entry<Document, Map<String, Integer>> entry : resultMapByDocument.entrySet()) {
                double similarity = calculateCosineSimilarity(entry.getValue(), queryVector);
                if (similarity > 0) { // solo documenti con similarità maggiore di 0
                    corrispondenzaSimiliarita.put(entry.getKey(), similarity);
                }
            }

            System.out.println(corrispondenzaSimiliarita.toString());
            List<Map.Entry<Document, Double>> sortedSimilarities = corrispondenzaSimiliarita.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).toList();
            List<Document> sortedDocuments = sortedSimilarities.stream()
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
            tableView.setItems(FXCollections.observableArrayList(sortedDocuments));

        }
    }





    private static void aggiungiParoleAlVocabolario(String stringa) {
        Arrays.stream(stringa.split("\\s+"))
                .forEach(parola -> vocabolario.putIfAbsent(parola, 0));
    }
    
    private static Map<String, Integer> textToVector(String stringa, boolean isTitle) {
        Map<String, Integer> textVector = new TreeMap<>(vocabolario);
        int weight = isTitle ? 2 : 1;
        Arrays.stream(stringa.split("\\s+"))
                .forEach(parola -> textVector.put(parola, textVector.getOrDefault(parola, 0) + weight));
        return textVector;
    }
    
    
    private static double calculateCosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        double scalProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String key : vector1.keySet()) {
            int val1 = vector1.get(key);
            int val2 = vector2.getOrDefault(key, 0); //frequenza di key in vector2, 0 se non esiste

            scalProduct += val1 * val2;
            normA += val1 * val1;
            normB += val2 * val2;
        }

        // Calcola la norma di vector2 per termini non presenti in vector1
        for (String key : vector2.keySet()) {
            if (!vector1.containsKey(key)) {
                int val = vector2.get(key);
                normB += val * val;
            }
        }


        return  (scalProduct / (Math.sqrt(normA) * Math.sqrt(normB)));


    }

    // questo metodo attualmente inizializza le stopwords da noi decise
    public void loadStopwords() throws IOException {
        File stopwordsFile = new File("stopwords-it.txt");
        if (stopwordsFile.exists()) {
            this.stopwords = Files.readAllLines(stopwordsFile.toPath());
        } else {
            this.stopwords = new ArrayList<>();
            System.out.println("Stopwords file not found.");
        }
    }

    private String removeStopwords(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        // Rimuove tutti gli apostrofi
        input = input.replaceAll("'", " ");

        // Dividi la stringa in parole e rimuovi le stopwords
        List<String> words = Stream.of(input.toLowerCase().split("\\s+"))
                .filter(word -> !stopwords.contains(word))
                .collect(Collectors.toList());

        return String.join(" ", words);
    }



    @FXML
    private void folderSelection(ActionEvent event) throws IOException {

        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona una cartella");
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            System.out.println("Cartella selezionata: " + selectedDirectory.getAbsolutePath());
            File[] files = selectedDirectory.listFiles();
            if (files != null) {
                List<Document> documents = new ArrayList<>();
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {
                        String title = null;
                        StringBuilder body = new StringBuilder();
                        try (BufferedReader bfr = new BufferedReader(new FileReader(file))) {
                            title = bfr.readLine(); // Assume la prima linea come titolo
                            String line;
                            while ((line = bfr.readLine()) != null) {
                                body.append(line).append("\n");
                            }

                            //aggiungo parole al vocabolario, pulite, no punteggiatura e no parole inutili
                            aggiungiParoleAlVocabolario(removeStopwords(title + " " + body.toString().replaceAll("[^\\s\\p{L}0-9]", " "))); // da controllare
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                        //aggiungo documento così com'è, con punteggiatura e stopwords
                        documents.add(new Document(title, body.toString()));
                    }
                }
                for (Document documentVocab : documents){
                    String cleanedTitle = removeStopwords(documentVocab.getTitle());
                    String cleanedBody = removeStopwords(documentVocab.getDocument_text().replaceAll("[^\\s\\p{L}0-9]", "")); //qui non mi torna
                    Map<String, Integer> titleVector = textToVector(cleanedTitle, true);
                    Map<String, Integer> bodyVector = textToVector(cleanedBody, false);
                    Map<String, Integer> documentVector = mergeVectors(titleVector, bodyVector);

                    resultMapByDocument.put(documentVocab, documentVector);
                }
                //System.out.println(resultMapByDocument.keySet());
                 System.out.println(vocabolario);


                tableView.setItems(FXCollections.observableArrayList(documents));
            }
            pane1.setVisible(false);
            pane2.setVisible(true);
        } else {
            System.out.println("Operazione annullata dall'utente.");
        }
    }


    private static Map<String, Integer> mergeVectors(Map<String, Integer> titleVector, Map<String, Integer> bodyVector) {
        Map<String, Integer> mergedVector = new TreeMap<>(titleVector);
        bodyVector.forEach((key, value) -> mergedVector.merge(key, value, Integer::sum));
        return mergedVector;
    }

    private void selezionaDocumento(){ //al doppio click su un documento della tableview, apre il documento selezionato

        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                pane2.setVisible(false);
                paneDocumento.setVisible(true);
                Document documentoSelezionato = tableView.getSelectionModel().getSelectedItem();
                if (documentoSelezionato != null) {
                    mostraContenutoDocumento(documentoSelezionato);
                }
            }
        });

    }

    private void mostraContenutoDocumento(Document documentoSelezionato) { //mostra nella textArea il corpo del documento selezionato

        String content = documentoSelezionato.getDocument_text();
        corpoDocumento.setText(content);
        mostrastatisticheDocumento(documentoSelezionato);

    }
    @FXML
    public void chiudiDocumento(){
        paneDocumento.setVisible(false);
        pane2.setVisible(true);
    }

    @FXML
    public void mostrastatisticheDocumento(Document documentoSelezionato){
            String testoDocumento = documentoSelezionato.getDocument_text();
            Map<String, Integer> documentVector = textToVector(testoDocumento.replaceAll("[!\"()*,.:;'?]", " ").toLowerCase(), false);
        // il problema resta nella creazione del vocabolario, perche le parole non sono correttamente separate!
        // penso che la logica di suddivisione vada risolta già in creazione
            System.out.println(documentVector);
            int totalWords = documentVector.values().stream().mapToInt(Integer::intValue).sum(); //WORKA
            int uniqueWords =  (int) documentVector.values().stream().filter(values -> values > 0).count();


            documentVector = textToVector(removeStopwords(testoDocumento.replaceAll("[!\"()*,.:;'?]", " ").toLowerCase()), false);
            int filteredWords = documentVector.values().stream().mapToInt(Integer::intValue).sum();
            int stopWords = totalWords - filteredWords;
            List<Map.Entry<String, Integer>> commonWords = documentVector.entrySet().stream()
                    .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                    .limit(5)
                    .toList();

            StringBuilder statsMessage = new StringBuilder();
            statsMessage.append("Numero totale di parole presenti: ").append(totalWords).append("\n");
            statsMessage.append("Numero di parole uniche presenti: ").append(uniqueWords).append("\n");
            statsMessage.append("Numero di stopword presenti: ").append(stopWords).append("\n");
            double percentage = ((double) stopWords / totalWords) * 100;
            String formattedPercentage = String.format("%.2f", percentage);
            statsMessage.append("Percentuale stopwords presenti pari a: ").append(formattedPercentage).append("%\n");
            statsMessage.append("Le 5 parole più comuni sono:\n");
            commonWords.forEach(entry -> statsMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

            statisticheDocumento.setText(statsMessage.toString());

    }





}


