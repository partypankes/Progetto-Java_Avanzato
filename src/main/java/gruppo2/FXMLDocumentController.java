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
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
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
    private TextField queryTf;
    
    @FXML
    private TableView<Document> tableView;

    @FXML
    private TableColumn<Document, String> titleColumn;
    
    private static TreeMap<String, Integer> vocabolario = new TreeMap<>();
    
    private static Map<String, Double> corrispondenzaSimiliarita = new TreeMap<>(Collections.reverseOrder());

    private List<String> stopwords;
   
    
    
    
    @Override
    public void initialize(URL url, ResourceBundle rb) {

        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));

    }    

    @FXML
    private void handleQuery(ActionEvent event) {
         
        Scanner scanner = new Scanner(System.in);
        System.out.println("Inserisci la prima stringa");
        String stringaUno = scanner.nextLine();
        System.out.println("Inserisci la seconda stringa");
        String stringaDue = scanner.nextLine();
        aggiungiParoleAlVocabolario(stringaUno);
        aggiungiParoleAlVocabolario(stringaDue);
        aggiungiParoleAlVocabolario(queryTf.getText());
        
        Map<String, Integer> vectorUno = textToVector(stringaUno);
        Map<String, Integer> vectorDue = textToVector(stringaDue);
        Map<String, Integer> queryVector = textToVector(queryTf.getText());
        
        
        corrispondenzaSimiliarita.put("Documento 1 ", calculateCosineSimilarity(vectorUno, queryVector));
        corrispondenzaSimiliarita.put("Documento 2 ", calculateCosineSimilarity(vectorDue, queryVector));

        List<Map.Entry<String, Double>> prova = corrispondenzaSimiliarita.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).toList();
       
        
        System.out.println("Similarità in ordine decrescente: \n\n" + prova);
        
    }
    
    
    
    
    private static void aggiungiParoleAlVocabolario(String stringa) {
        Arrays.stream(stringa.split("\\s+"))
                .forEach(parola -> vocabolario.putIfAbsent(parola, 0));
    }
    
    private static Map<String, Integer> textToVector(String stringa) {
        Map<String, Integer> textVector = new TreeMap<>(vocabolario);
        Arrays.stream(stringa.split("\\s+"))
                .forEach(parola -> textVector.put(parola, textVector.getOrDefault(parola, 0) + 1));
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
        this.stopwords = Files.readAllLines(Paths.get("stopwords-it.txt"));
    }


    @FXML
    private void folderSelection(ActionEvent event) throws IOException {
        
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona una cartella");
        File selectedDirectory = directoryChooser.showDialog(null);
        if (selectedDirectory != null) {
            System.out.println("Cartella selezionata: " + selectedDirectory.getAbsolutePath());
            File [] files = selectedDirectory.listFiles();
            if (files != null) {
                List<Document> documents = new ArrayList<>();
                for (File file : files) {
                    if (file.isFile() && file.getName().endsWith(".txt")) {


                            StringBuilder result = new StringBuilder();
                            try(BufferedReader bfr = new BufferedReader(new FileReader(file))){
                                String line = bfr.readLine();
                                while ((line = bfr.readLine()) != null) {
                                    result.append(line).append("\n");
                                    System.out.println(result);
                                }
                            } catch (IOException e) {
                                throw new RuntimeException(e);
                            }



                        documents.add(new Document(readTitle(file.getAbsolutePath()),result.toString().replaceAll("[^\\s\\p{L}0-9]", "")));
                        System.out.println(file.getAbsolutePath());
                        System.out.println(documents.size());
                    }
                }
                ObservableList<Document> documentObservableList = FXCollections.observableArrayList(documents);
                tableView.setItems(documentObservableList);

                try {
                    loadStopwords();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

                for (Document document : documents) {
                    //In questa parte di codice vengono eliminate le stopwords e viene creato "il vettore" per ogni documento(per ora viene stampato). Da implementare la logica di vocabolario e rifinire questa per poi aggiustare il tutto con la query


                    ArrayList<String> allWords = Stream.of(document.getDocument_text().toLowerCase().split(" ")).collect(Collectors.toCollection(ArrayList<String>::new));
                    allWords.removeAll(stopwords);

                    String result = String.join(" ", allWords);

                    System.out.println(textToVector(result));
                    //implementare l'aggiunta al vocabolario
                }







            }

// gestire eccezioni
            pane1.setVisible(false);
            pane2.setVisible(true);
        } else {
            System.out.println("Operazione annullata dall'utente.");
        }
        }


        public String readTitle (String nomeFile){
        try(BufferedReader bfr = new BufferedReader(new FileReader(nomeFile))){
            return bfr.readLine(); //si dovrebbe verificare che il file abbia una riga
            } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        }


    }

