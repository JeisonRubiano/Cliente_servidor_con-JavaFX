import javafx.concurrent.Task;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;

public class Servidor extends Application {
    private String serverIP = getServerIP(); // IP del servidor
    private int serverPort = 12345; // Puerto predeterminado
    private ExecutorService executorService;
    private ServerSocket serverSocket;

    @Override
    public void start(Stage primaryStage) {
        Label ipLabel = new Label("IP del servidor: " + serverIP);
        Label portLabel = new Label("Puerto del servidor: " + serverPort);

        Button showImagesButton = new Button("Mostrar Imágenes");
        showImagesButton.setOnAction(e -> {
            showImages();
        });

        VBox root = new VBox(10);
        root.getChildren().addAll(ipLabel, portLabel, showImagesButton);

        Scene scene = new Scene(root, 300, 200);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Servidor");
        primaryStage.show();

        // Iniciar el servicio del servidor en un hilo separado
        Task<Void> serverTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                startServer();
                return null;
            }
        };
        new Thread(serverTask).start();
    }

    private void startServer() {
        try {
            serverSocket = new ServerSocket(serverPort);
            System.out.println("Servidor iniciado. Esperando conexiones...");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress().getHostAddress());

                // Manejar cada cliente en un hilo separado
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                executorService.execute(clientHandler);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getServerIP() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            e.printStackTrace();
            return "Desconocido";
        }
    }

    private void showImages() {
        // Ruta de la carpeta donde se encuentran las imágenes en el servidor
        String folderPath = "Data/";

        File folder = new File(folderPath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles != null) {
            Stage stage = new Stage();
            VBox vbox = new VBox();
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    Image image = new Image(file.toURI().toString());
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(200);
                    imageView.setFitHeight(200);
                    vbox.getChildren().add(imageView);
                }
            }
            Scene scene = new Scene(vbox);
            stage.setScene(scene);
            stage.setTitle("Imágenes del Servidor");
            stage.show();
        }
    }

    @Override
public void stop() throws Exception {
    super.stop();
    if (serverSocket != null && !serverSocket.isClosed()) {
        serverSocket.close();
    }
    if (executorService != null && !executorService.isShutdown()) {
        executorService.shutdown();
    }
}


    public static void main(String[] args) {
        launch(args);
    }

    // Clase interna ClientHandler
    public class ClientHandler implements Runnable {
        private Socket clientSocket;

        public ClientHandler(Socket clientSocket) {
            this.clientSocket = clientSocket;
        }

        @Override
        public void run() {
            try {
                System.out.println("Cliente conectado desde: " + clientSocket.getInetAddress().getHostAddress());

                // Flujo de entrada y salida para comunicarse con el cliente
                DataInputStream dis = new DataInputStream(clientSocket.getInputStream());
                DataOutputStream dos = new DataOutputStream(clientSocket.getOutputStream());

                // Recibir la solicitud del cliente
                String request = dis.readUTF();
                

                if (request.equals("UPLOAD_IMAGE")) {
                    // Si la solicitud es para subir una imagen
                    receiveImage(dis);
                } else if (request.equals("LIST_IMAGES")) {
                    // Si la solicitud es para listar las imágenes almacenadas
                    sendImageList(dos);
                }

            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void receiveImage(DataInputStream dis) throws IOException {
            // Recibir el nombre del archivo y los datos de la imagen
            String fileName = dis.readUTF();
            long fileSize = dis.readLong();

            // Crear un flujo de salida para escribir los datos en el archivo
            FileOutputStream fos = new FileOutputStream("Data/" + fileName);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = dis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.close();
        }

        private void sendImageList(DataOutputStream dos) throws IOException {
            // Enviar la lista de nombres de archivo de las imágenes almacenadas
            File folder = new File("Data/");
            System.out.println("Se envian las imagenes al cliente");
            File[] listOfFiles = folder.listFiles();
            if (listOfFiles != null) {
                for (File file : listOfFiles) {
                    if (file.isFile()) {
                        dos.writeUTF(file.getName());
                    }
                }
            }
            // Enviar una señal de fin de lista
            dos.writeUTF("END");
        }
    }
}
