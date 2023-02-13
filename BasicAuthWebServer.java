import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Base64;

public class BasicAuthWebServer {
    /*run the http server on this TCP port. */
    private static final int PORT = 8080;

    /* The socket used to process incoming connections 
    from a web clients*/
    private static ServerSocket dServerSocket;

    public BasicAuthWebServer () throws Exception {
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
        try{ 
            StringTokenizer st =
                new StringTokenizer (request, " ");
            command = st.nextToken();
            pathname = st.nextToken();
        } catch (Exception e) {
            osw.write ("HTTP/1.0 400 Bad Request\n\n");
            osw.close();
            return;
        }

        if (command.equals("GET")) {
            // If the request is a GET, try to respond with the file the user is requesting. //
            Credentials c = getAuthorization(br);
            if ((c != null) && (MiniPasswordManager.checkPassword(c.getUsername(), c.getPassword()))) {
                serveFile (osw,pathname);        
            } else {
                osw.write ("HTTP/1.0 401 Unauthorized");
                osw.write ("WWW-Authenticate: Basic realm=BasicAuthWebServer");
            }
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
    private Credentials getAuthorization (BufferedReader br) {
        try {
            String header = null;
            while (!(header = br.readLine()).equals("")) {
                if (header.startsWith("Authorization:")) {
                    StringTokenizer st = new StringTokenizer(header, " ");
                    st.nextToken(); //skip "Authorization"
                    st.nextToken(); // skip "Basic"
                    return new Credentials(st.nextToken());
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    public void serveFile (OutputStreamWriter osw, String pathname) throws Exception {
        FileReader fr = null;
        int c = -1;
        int sentBytes = 0;

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
        while ( (c != -1) && (sentBytes < 1000000) ){
            osw.write (c);
            sentBytes++;
            c = fr.read();
            if (sentBytes > 1000000) {
                logEntry("403Error.txt", "File Exceeds Size Limit");
                throw new Exception("403 Forbidden");
            }
        }
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
        if (argv.length == 1) {
            /* Initialize MiniPasswordManager */
            MiniPasswordManager.init(argv[0]);
            /* Create a BasicAuthWebServer object, and run it */
            BasicAuthWebServer baws = new BasicAuthWebServer();
            baws.run();
        } else {
            System.err.println ("Usage: java BasicAuthWebServer <pwdfile>");
        }
    }
}
class Credentials {
    private String dUsername;
    private String dPassword;
    public Credentials(String authString) throws Exception {
	    authString = new String((Base64.getDecoder().decode(authString)));
	    StringTokenizer st = new StringTokenizer(authString, ":");
	    dUsername = st.nextToken();
	    dPassword = st.nextToken();
    }
    public String getUsername() {
	    return dUsername;
    }
    public String getPassword() {
	    return dPassword;
    }
}
