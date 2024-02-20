import javafx.application.Application;
import javafx.application.Platform;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;
import javafx.concurrent.Task;

public class Cliente extends Application {
    private String serverIP = "192.168.1.55"; // IP predeterminada
    private int serverPort = 12345; // Puerto predeterminado

    @Override
    public void start(Stage primaryStage) {
        Label serverIpLabel = new Label("IP del Servidor:");
        TextField serverIpTextField = new TextField();
        serverIpTextField.setText(serverIP);

        Label portLabel = new Label("Puerto:");
        TextField portTextField = new TextField();
        portTextField.setText(String.valueOf(serverPort));

        Button connectButton = new Button("Conectar");
        connectButton.setOnAction(e -> {
            serverIP = serverIpTextField.getText();
            serverPort = Integer.parseInt(portTextField.getText());
            if (connectToServer()) {
                showAlert(AlertType.INFORMATION, "Conexión exitosa", "La conexión con el servidor fue exitosa.");
            } else {
                showAlert(AlertType.ERROR, "Error de conexión", "No se pudo establecer conexión con el servidor.");
            }
        });

        Button uploadButton = new Button("Subir Imagen");
        Button showImagesButton = new Button("Mostrar Imágenes");

        uploadButton.setOnAction(e -> {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    selectAndUploadImage();
                    return null;
                }
            };
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        showImagesButton.setOnAction(e -> {
            Task<Void> task = new Task<Void>() {
                @Override
                protected Void call() throws Exception {
                    showImages();
                    return null;
                }
            };
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();
        });

        VBox root = new VBox(10);
        HBox serverIpBox = new HBox(10, serverIpLabel, serverIpTextField);
        HBox portBox = new HBox(10, portLabel, portTextField);
        root.getChildren().addAll(serverIpBox, portBox, connectButton, uploadButton, showImagesButton);

        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Cliente");
        primaryStage.show();
    }

    private void selectAndUploadImage() {
        System.out.println("Selecion de imagen");
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Seleccionar Imagen");
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            uploadImage(selectedFile);
        }
    }

    private void uploadImage(File file) {
        try {
            Socket socket = new Socket(serverIP, serverPort);
            DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
            System.out.println("Envio informacion de imagen al servidor");
            // Envía el nombre del archivo al servidor
            dos.writeUTF(file.getName());

            // Envía el contenido del archivo al servidor
            byte[] buffer = new byte[4096];
            try (DataInputStream dis = new DataInputStream(socket.getInputStream());
                 FileInputStream fis = new FileInputStream(file)) {
                int bytesRead;
                while ((bytesRead = fis.read(buffer)) != -1) {
                    dos.write(buffer, 0, bytesRead);
                }
            }

            System.out.println("Imagen subida correctamente.");

            dos.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void showImages() {
        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                Socket socket = null; // Definir la variable socket
                try {
                    if (!connectToServer()) {
                        Platform.runLater(() -> showAlert(AlertType.ERROR, "Error de conexión", "No se pudo establecer conexión con el servidor."));
                        return null;
                    }
    
                    socket = new Socket(serverIP, serverPort); // Inicializar la variable socket
    
                    DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
                    DataInputStream dis = new DataInputStream(socket.getInputStream());
    
                    dos.writeUTF("LIST_IMAGES");              
                    System.out.println("Solicita las imagenes la servidor");

                    ArrayList<String> imageNames = new ArrayList<>();
                    String imageName;
                    
                    while (!(imageName = dis.readUTF()).equals("END")) {

                        System.out.println(imageName);
                        System.err.println("Conexion con la ip"+ serverIP+"socket "+ serverPort);
                        imageNames.add(imageName);
                    }
    
                    if (!imageNames.isEmpty()) {
                        System.out.println("entra if de validacion de imagenes");
                        Platform.runLater(() -> {
                            Stage stage = new Stage();
                            TilePane tilePane = new TilePane();
                            tilePane.setPrefColumns(3);
                            for (String name : imageNames) {
                                System.out.println("muestra imagenes");
                                ImageView imageView = new ImageView(new Image("http://" + serverIP + ":" + serverPort + "/" + name));
                                imageView.setFitWidth(150);
                                imageView.setFitHeight(150);
                                tilePane.getChildren().add(imageView);
                            }
    
                            Scene scene = new Scene(tilePane);
                            stage.setScene(scene);
                            stage.setTitle("Imágenes del Servidor");
                            stage.show();
                        });
                    } else {
                        System.out.println("No hay imágenes en el servidor.");
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    Platform.runLater(() -> showAlert(AlertType.ERROR, "Error", "Se produjo un error al intentar mostrar las imágenes."));
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                return null;
            }
        };
    
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    

    private boolean connectToServer() {
        try {
            Socket socket = new Socket(serverIP, serverPort);
            socket.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private void showAlert(AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
