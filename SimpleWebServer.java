import java.io.*;
import java.net.*;
import java.util.*;

public class SimpleWebServer {
    /*run the http server on this TCP port. */
    private static final int PORT = 8080;

    /* The socket used to process incoming connections 
    from a web clients*/
    private static ServerSocket dServerSocket;

    public SimpleWebServer () throws Exception {
        dServerSocket = new ServerSocket (PORT);
    }

    public void run() throws Exception {
        while (true) {
            /* Wait for a connections from a client. */
            Socket s  = dServerSocket.accept();
            /* Then, process the client's request. */
            processRequest(s);
        }
    }
    /* Reads the HTTP request from the client and
    responds with the file the user requested or an 
    HTTP error code */
    public void processRequest(Socket s) throws Exception {
        // Used to write data from the client //
        BufferedReader br = 
            new BufferedReader (
                new InputStreamReader (s.getInputStream()));

        // Used to write data to the client. //
        OutputStreamWriter osw = 
            new OutputStreamWriter (s.getOutputStream());

        // Read the HTTP request  from the client. //
        String request = br.readLine();

        String command = null;
        String pathname =null;

        // Parse the HTTP request.//

        StringTokenizer st =
            new StringTokenizer (request, " ");

        command = st.nextToken();
        pathname = st.nextToken();

        if (command.equals("GET")) {
            // If the request is a GET, try to respond with the file the user is requesting. //
            serveFile (osw,pathname);

        } 
        else if (command.equals("PUT")) {
            storeFile(br,osw,pathname);
            logEntry("logFile.txt",command);
        }
        else {
            // if the request is NOT a GET or PUT, return an error saying this serveer does not implement the requested command//
            osw.write ("HTTP/1.0 501 Not Implemented\n\n");
        }

        // Close the connection to the client. //
        osw.close();
    }

    public void serveFile (OutputStreamWriter osw, String pathname) throws Exception {
        FileReader fr = null;
        int c = -1;
        StringBuffer sb = new StringBuffer(); 

        // Remove the initial slash at the beginning of the pathname in the request //
        if (pathname.charAt(0) == '/')
            pathname = pathname.substring(1);
        
        // If there was no filename specified by the client, serve the 'index.html' file. //
        if (pathname.equals(""))
            pathname = "C:\\Users\\Serena\\Desktop\\test\\index.html";

        // Try to open file specified by pathname. //
        try {
            fr = new  FileReader (pathname);
            c = fr.read();
        }
        catch (Exception e) {
            // if the file is not found, return the appropriate HTTP response code. //
            osw.write ("HTTP/1.0 404 Not Found\n\n");
            return;
        }

        // if the requested file can be successfully opened and read, then return an OK response code and sent the contents of the file. //
        osw.write ("HTTP/1.0 200 OK\n\n");
        while (c != -1) {
            sb.append((char)c);
            c = fr.read();
        }
        osw.write (sb.toString());
    }
    public void storeFile(BufferedReader br,
            OutputStreamWriter osw,
            String pathname) throws Exception {
        FileWriter fw = null;
        try {
            fw = new FileWriter (pathname);
            String s = br.readLine();
            while (s != null) {
                fw.write (s);
                s = br.readLine();
            }
            fw.close();
            osw.write ("HTTP/1.0 201 Created");
        }
        catch (Exception e) {
            osw.write ("HTTP/1.0 500 Internal Server Error");
        }
    }
    public void logEntry(String filename,String record) {
        
        try {
            FileWriter fw = new FileWriter (filename, true);
            fw.write (getTimestamp() + " " + record);
            fw.close(); 
        }
        catch (Exception e) {

        }
    }
    public String getTimestamp() {
        return (new Date()).toString();
    }

    // This method is called when the program is run from the command line. // 
    public static void main (String argv[]) throws Exception {

        // create a SimpleWebServer object and run it //
        SimpleWebServer sws = new SimpleWebServer();
        sws.run();
    }
}