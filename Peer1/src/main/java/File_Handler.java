

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * Created by kmursi on 3/14/17.
 */
public class File_Handler {

    ObjectOutputStream out;
    Socket socket;

    /*********************************************************************************************/

    boolean isValidName(String text)   //this method used to check the validity of the file name
    {
        if(text.contains("."))         //file name must contains '.'
        {
            String[] textArray = text.split(Pattern.quote(".")); //split
            if(textArray.length==2)    //has two sides
            {
                if (!(textArray[0].equals(null)) && !(textArray[1].equals(null))) //firs side and second side are not empty
                    return true;
            }
        }
        return false;
    }

    /*********************************************************************************************/

    boolean isValidDownloadName(String text)    //this method used to check the validity of the download request format
    {
        if(text.contains("-"))                  //split by '-'
        {
            String[] textArray = text.split(Pattern.quote("-"));

            if (textArray.length == 3)          //must contains three sides
            {
                if(isValidName(textArray[2]))   // the third side which contains the file name must be valid
                {
                    if (!(textArray[0].equals(null)) && !(textArray[1].equals(null))&& !(textArray[2].equals(null))) //the three sides are not empty
                        return true;
                }
            }

        }
        return false;
    }

    /*********************************************************************************************/

    public void Register_a_File(String fileName)          //Register in the current machine
    {
        if(file_exist(fileName))
        {
            Main.files.add(fileName);                     // add the file to the local registration list
            File f = new File(Main.folder+"/"+fileName);
            FileLastUpdate watcher = new FileLastUpdate(f,f.lastModified(),0);
            Main.filesWatchers.add(watcher);              //add the file to the update tracker list
            System.out.println("File "+fileName+" has been registered successfully !\n");
        }
        else
        {
            System.out.println("File is not existed on your local folder !\n");
        }
    }

    /*********************************************************************************************/

    boolean file_exist(String fileName) {                              //check the file existence on the working directory
        File folder = new File(Main.folder);                           //get the resources folder path
        File[] listOfFiles = folder.listFiles();                       //store files into file array
        for (int i = 0; i < listOfFiles.length; i++) {                 //loop through each of the files looking for filenames that match
            String filename = listOfFiles[i].getName();                //store the file name
            if (filename.startsWith(fileName)) {                       //if exist, return true
                return true;
            }

        }
        return false;
    }

    /*********************************************************************************************/

    public void Search_for_a_File(String fileName)                  //search file on the topology
    {                                                               //search is done by creating a search message has 10 TTL
        try{
            Message message = new Message(Main.peerID,Main.peerIP,"search","",10, Integer.parseInt(Main.peerPort), fileName.trim(),"");
            message.set_PK();
            System.out.println("Search message is created for file "+fileName);
            broadcast(message);                                     //broadcast the search message
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    /*********************************************************************************************/
    public void broadcast(Message message)
    {
        boolean flag = true;                                        //flag indicate the message validity
        /////////////////////////////////////////////////////////////////////////////
        if(flag) {
            Main.messageTrack.add(message);                         //add the message to the received message pool
            if(message.TTL>0) {                                     //check the validity of the TTL
                try {
                    message.set_TTL("-");
                    message.append_header(Main.peerID);
                    Socket conn;
                    ObjectOutputStream send;
                    /////////////////////////////////////////////////////////////////////////////
                    for (int i = 0; i < Main.neighborList.size(); i++) {
                        conn = new Socket(Main.neighborList.get(i).neighborIP, Main.neighborList.get(i).neighborPort);
                        /////////////////////////////////////////////////////////////////////////////
                        send = new ObjectOutputStream(conn.getOutputStream());//initiate writer
                        send.flush();
                        send.writeObject(message);                             //send the message to all neighbors
                        send.flush();
                        send.close();
                        conn.close();
                        System.out.println("Broadcast message is sent to "+Main.neighborList.get(i).neighborIP+"-"+Main.neighborList.get(i).neighborPort+"!\n");
                    }
                    /////////////////////////////////////////////////////////////////////////////
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            else
            {
                System.out.println("Message "+message.id+" dropped because its TTL exceeded the limit!");
            }
        }
        else
        {
            System.out.println("Message "+message.id+" dropped because it was received before !");
        }
    }

    /*********************************************************************************************/

    public void Create_Local_File(String fileName,String fileContent) //write the downloaded file into the local director
    {
        try
        {
            File f = new File(Main.folder);                           //get the resources folder path

            FileWriter writer = new FileWriter(f+"/"+fileName.trim(),true);//initiate writer
            /////////////////////////////////////////////////////////////////////////////
            writer.write(fileContent);                                //write
            writer.close();                                           //close writer
        }
        catch(UnknownHostException unknownHost){                      //To Handle Unknown Host Exception
            System.err.println("host not available..!");
        }
        catch(IOException ioException){                               //To Handle Input-Output Exception
            ioException.printStackTrace();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }

    /*********************************************************************************************/

    public void Download_From_Peer()                                  //download file from a peer
    {
        Scanner uInput = new Scanner(System.in);
        String fileName;
        String ip=null;
        int peerPort = 0;
        /////////////////////////////////////////////////////////////////////////////
        System.out.println("Enter the peer id, and file name using this format (p1-file.txt):");
        String uString = uInput.nextLine();                           //get the user input
        /////////////////////////////////////////////////////////////////////////////
        try
        {
                String [] textArray = uString.split(Pattern.quote("-")); //split
                fileName = textArray[1];
                for (int i = 0; i < Main.peersList.size(); i++) {
                    if(Main.peersList.get(i).neighborID.equals(textArray[0])) {
                        ip = Main.peersList.get(i).neighborIP;
                        peerPort = Main.peersList.get(i).neighborPort;    //second side contains ip address

                    }
                System.out.println("File name:"+fileName);
                /////////////////////////////////////////////////////////////////////////////
                if(!(peerPort==Main.port)) {
                    socket = new Socket(ip, peerPort);                    //initiate client socket
                    System.out.println("\nConnected to peer : " + ip + " through port : " + peerPort + "\n");
                    out = new ObjectOutputStream(socket.getOutputStream());//initiate writer
                    out.flush();
                    out.writeObject(new Message(Main.peerID,Main.peerIP,"download","",10, Integer.parseInt(Main.peerPort), fileName.trim(),""));                             //write
                    out.flush();
                    /////////////////////////////////////////////////////////////////////////////
                    ObjectInputStream in = new ObjectInputStream(socket.getInputStream()); //initiate reader
                    String fileContent = in.readObject().toString();       //read and store into content
                    /////////////////////////////////////////////////////////////////////////////
                    if (fileContent.trim().equals("File not found".trim())) //check file existence in the peer
                    {
                        System.out.println("File not found");
                    } else {
                        Create_Local_File(fileName, fileContent);          //call create file function and attache file name and content
                        System.out.println(fileName + " has been downloaded successfully\n");
                        Main.files.add(fileName);
                    }
                    /////////////////////////////////////////////////////////////////////////////
                    in.close();                                            //close reader
                    out.close();                                           //close writer
                    socket.close();                                        //close connection
                }
                else
                {
                    System.out.println("You are not allowed to download from the current peer that you are using !\n");
                }
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

    /*********************************************************************************************/

    public void push(String fileName,int version)       //this method push teh update to all peers
    {
        try{
                                                        //new invalidate message carries the invalid file name
            Message message = new Message(Main.peerID,Main.peerIP,"invalidate","",10, Integer.parseInt(Main.peerPort), fileName.trim(),"");
            message.set_file_version_number(version);   //increment the file version number by 1
            message.set_PK();                           //set primary key for the message
            System.out.println("Invalidate query is created for file "+fileName +" version number "+message.fileVersionNumber);
            broadcast(message);                         //broadcast the message
        }

        catch (Exception e) {
            e.printStackTrace();
        }
    }

}


