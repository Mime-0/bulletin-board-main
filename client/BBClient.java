import java.io.*;
import java.net.Socket;

public class BBClient {
    private final String host;
    private final int port;

    public BBClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void run() {
        try (
            Socket socket = new Socket(host, port);
            BufferedReader serverIn =
                new BufferedReader(new InputStreamReader(socket.getInputStream()));
            PrintWriter serverOut =
                new PrintWriter(socket.getOutputStream(), true);
            BufferedReader userIn =
                new BufferedReader(new InputStreamReader(System.in))
        ) {
            System.out.println("Connected to " + host + ":" + port);

            String line;
            while ((line = userIn.readLine()) != null) {
                serverOut.println(line);

                String response;
                while ((response = serverIn.readLine()) != null) {
                    System.out.println(response);
                    if (response.startsWith("OK") || response.startsWith("ERROR")) {
                        break;
                    }
                }

                if (line.equalsIgnoreCase("DISCONNECT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.out.println("Client error: " + e.getMessage());
        }
    }
}
