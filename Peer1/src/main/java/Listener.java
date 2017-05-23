

/**
 * Created by kmursi on 3/14/17.
 */

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

//PeerServer
class Listener extends Thread{
    int port;
    Message message;
    ServerSocket server;
    Socket connection;
    BufferedReader br = null;
    ObjectInputStream in;
    /*********************************************************************************************/
    public Listener(Socket s , int port) {                 //listener constructor receives socket object and port number
        connection=s;
        this.port = port;
    }
    /*********************************************************************************************
     * listen to the port
     * based on the command id, redirect the message to a proper method
     * if this message received before, drop it
     */

    public synchronized void run() {
        try{
            boolean flag = true;                                            //this flag used to know the listener type
            String peerIP = connection.getInetAddress().getHostName();      //get the connecting node IP
            System.out.println("** Peer " + peerIP + " connected..\n");
            in = new ObjectInputStream(connection.getInputStream());        //initiate reader
            message = (Message) in.readObject();
            /////////////////////////////////////////////////////////////////////////////
            for (Message m : Main.messageTrack) {                           //check if the received message had been received before
                if (m.PK.equals(message.PK))
                    flag = false;
            }
            /////////////////////////////////////////////////////////////////////////////
            if (flag) {
                if (message.command.equals("search")) {                     //command = search, go to search method
                    search();
                } else if (message.command.equals("download")) {            //command = download, go to download method
                    download_listener();
                } else if (message.command.equals("found")) {               //command = found, go to search method
                    forward();
                } else if (message.command.equals("invalidate")) {          //command = invalidate, go to search method
                    invalidate();
                }
                in.close();
                connection.close();
            }
            else{
                System.out.println("Message "+message.PK+" has been dropped !");
            }
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*********************************************************************************************/
    /*
    this method listen to the download requests
    it sends back the request file
     */
    public void download_listener()
    {
        try {
            /////////////////////////////////////////////////////////////////////////////
            ObjectOutputStream out = new ObjectOutputStream(connection.getOutputStream()); //initiate writer
            out.flush();
            String content=null;
            /////////////////////////////////////////////////////////////////////////////
            try
            {
                final File f = new File(Main.folder);                                       //get the jar directory
                FileReader fr = new FileReader(f+ "/"+message.content.trim());     //Reads the filename into file reader
                BufferedReader br = new BufferedReader(fr);
                String value=new String();
                while((value=br.readLine())!=null)              //Appending the content read from the BufferedReader object until it is null and stores it in str
                    content=content+value+"\r\n";               //append the content out of the read lines
                br.close();
                fr.close();
                System.out.println("File "+message.content.trim()+" has been sent successfully to peer "+message.id);
            }
            catch(UnknownHostException unknownHost){            //To Handle Unknown Host Exception
                System.err.println("host not available..!");
            }
            catch(Exception e)
            {
                System.out.println("File not found");
                content="File not found".trim();                //file not found message will be sent to the peer
            }
            /////////////////////////////////////////////////////////////////////////////

            out.writeObject(content);                           //content sending to the peer
            out.flush();                                        //close writer
            in.close();                                         //close reader
            connection.close();                                 //close connection

        } catch(UnknownHostException unknownHost){              //To Handle Unknown Host Exception
            System.err.println("host not available..!");
        }
        catch(IOException ioException){                         //To Handle Input-Output Exceptions
            ioException.printStackTrace();
        } finally {
            Thread.currentThread().stop();                      //end the current thread
        }

    }
    /*********************************************************************************************/
    /*
    this method listens to the search queries
    it sends back the search result if the file found
    else, it rebroadcast the search query
     */
    public void search()
    {
        Neighbor n = null;
        try {
            if(Main.files.contains(message.content.trim()))                 //search for the requested file
            {
                System.out.println("File "+message.content.trim()+" has found in this peer ! \n");
                String [] splitter = message.header.split("-");       //split by -
                /////////////////////////////////////////////////////////////////////////////
                for(int i=0;i<Main.neighborList.size();i++)
                {
                    if(Main.neighborList.get(i).neighborID.equals(splitter[splitter.length-1]))
                    {
                        n = Main.neighborList.get(i);                       //get the neighbor information
                    }
                }
                /////////////////////////////////////////////////////////////////////////////
                Socket socket = new Socket(n.neighborIP,n.neighborPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); //initiate writer
                out.flush();
                //create new message to be sent
                Message m = new Message(message.id,message.ip,"found","File found in :"+Main.peerID,0,Integer.parseInt(Main.peerPort),message.content,message.header);
                out.writeObject(m);                                          //send the message
                out.flush();
                out.close();
                socket.close();
                System.out.println("Hit query sent back to the main searcher !\n");
                File_Handler f = new File_Handler();                        //create new object of file handler
            }
            /////////////////////////////////////////////////////////////////////////////
            else
            {
                System.out.println("File "+message.content+" not found on your machine !");
                File_Handler f = new File_Handler();
                f.broadcast(message);                                       //broadcast the search message when not found on the current peer
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    /*********************************************************************************************/
    /*
    this method handles the forwarding of a hit-message to the sam path that it had been sent through
     */
    public void forward()
    {
        Neighbor n = null;
        try {
            if(!message.id.equals(Main.peerID)){                      //if the message created by this peer
                message.cut_header();                                 //cut the path the is recorded in the header
                System.out.println(message.header);
                String[] splitter = message.header.split("-");  //split the header by -
                for (int i = 0; i < Main.neighborList.size(); i++) {
                    if (Main.neighborList.get(i).neighborID.equals(splitter[splitter.length-1])) {
                        n = Main.neighborList.get(i);
                    }
                }
                Socket socket = new Socket(n.neighborIP, n.neighborPort);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream()); //initiate writer
                out.flush();
                out.writeObject(message);                               //forward the message back
                out.flush();
                out.close();
                socket.close();
                System.out.println("Hit query forwarded back to " + n.neighborIP + " - " + n.neighborPort + "!\n");

            }
            else
            {
                System.out.println("File "+message.content+" found in peer "+message.data+". Time:"+System.currentTimeMillis());
            }

        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    /*********************************************************************************************/
    /*
    this method send received the update query when a master file changed
     */
    public void invalidate()
    {
        Neighbor n = null;
        try {
            if(Main.files.contains(message.content.trim()))    //the message content will carry the invalid file name
            {
                File f = new File(Main.folder+"/"+message.content.trim()); //get the invalid file
                if(f.exists()) {                                //found in the current machine ?
                    System.out.println("Invalidate copy query received from " + message.id + " !\n");
                    System.out.println("File " + message.content + " has been deleted !\n");
                    for (int i = 0; i < Main.files.size();i++)
                    {
                        if(Main.files.get(i).equals(f.getName()))
                        {
                            Main.files.remove(i);
                        }
                    }
                    f.delete();                             //delete the invalid version
                    Download_From_Peer();                   //download the updated version from the owner
                }
            }
            else                                            //if the file was not found
            {
                System.out.println("File "+message.content+" not found on your machine !");
                File_Handler f = new File_Handler();
                f.broadcast(message);                       //broadcast the message again
            }
        }
        catch(Exception e)
        {
            e.printStackTrace();
        }
    }
    /*********************************************************************************************/
    public void Download_From_Peer()                        //download file from a peer
    {
        String ip = null;
        int peerPort = 0;
        /////////////////////////////////////////////////////////////////////////////
        try {

            for (int i = 0; i < Main.peersList.size(); i++) {
                if (Main.peersList.get(i).neighborID.equals(message.id)) {
                    ip = Main.peersList.get(i).neighborIP;
                    peerPort = Main.peersList.get(i).neighborPort;      //second side contains ip address

                }
            }
            System.out.println("File name:" + message.content+" version number "+message.fileVersionNumber+".\n");
            /////////////////////////////////////////////////////////////////////////////
            if (!(peerPort == Main.port)) {
                Socket socket = new Socket(message.ip, message.port);   //initiate client socket
                System.out.println("\nConnected to peer : " + ip + " through port : " + peerPort + "\n");
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());//initiate writer
                out.flush();
                out.writeObject(new Message(Main.peerID, Main.peerIP, "download", "", 10, Integer.parseInt(Main.peerPort), message.content.trim(), ""));                             //write
                out.flush();
                /////////////////////////////////////////////////////////////////////////////
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream()); //initiate reader
                String fileContent = in.readObject().toString();       //read and store into content
                /////////////////////////////////////////////////////////////////////////////
                if (fileContent.trim().equals("File not found".trim())) //check file existence in the peer
                {
                    System.out.println("File not found");
                } else {
                    new File_Handler().Create_Local_File(message.content.trim(), fileContent);          //call create file function and attache file name and content
                    System.out.println(message.content.trim() + " update has been downloaded successfully, version number"+message.fileVersionNumber+"\n");
                    Main.files.add(message.content.trim());
                }
                /////////////////////////////////////////////////////////////////////////////
                in.close();                                            //close reader
                out.close();                                           //close writer
                socket.close();                                        //close connection
            } else {
                System.out.println("You are not allowed to download from the current peer that you are using !\n");
            }
        }
        catch(UnknownHostException unknownHost){                        //To Handle Unknown Host Exception
            System.err.println("host not available..!");
        }
        catch(IOException ioException){                                 //To Handle Input-Output Exception
            ioException.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        catch (Exception e) {
            e.printStackTrace();
        }



    }

}