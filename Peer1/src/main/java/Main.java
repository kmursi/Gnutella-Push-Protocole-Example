
import java.io.*;
import java.net.*;
import java.util.Collections;
import java.util.Scanner;
import java.util.List;
import java.util.ArrayList;

/*********************************************************************************************/
public class Main extends Thread {
    static final File f = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath()); //get the jar directory
    static File parentFolder = new File(f.getParent());                                     //get the parent folder of the jar
    static File resourcesFolder = new File(parentFolder.getParent()+"/src/main/");
    public static String masterFolder = parentFolder.getParent().toString();
    public static String folder = resourcesFolder.toString();
    public static String configFile = null;                       //config file
    public static String peerIP;                                  //hold peer ip
    public static String peerPort;                                //port number
    public static String peerID;                                  //p1, p2 or....pn
    public static List<Neighbor> neighborList = new ArrayList();  //this list store neighbors
    public static List<Neighbor> peersList = new ArrayList();     //this list stores the peers who are defined in the config file
    public static List<String> files = new ArrayList();           //this list stores the registered files locally
    public static List<FileLastUpdate> filesWatchers = new ArrayList(); //keep track the files changes
    public static List<Message> messageTrack = new ArrayList();   //keep track the received messages
    public static boolean flag=false;


    static int port;
    Main(int port)
    {
        this.port = port;
    }
    /*********************************************************************************************/
    public static void main(String[] args) throws InterruptedException {
        String userInput;                                           //define user input variable
        System.out.println("Enter '1'to apply the star topology or 2 for 2D-mesh topology:");
        Scanner uIn = new Scanner(System.in);
        userInput =uIn.nextLine().trim();
        if(userInput.equals("1"))                                   //if user input = 1, star topology config will be loaded
        {
            configFile= masterFolder+"/Config.txt";
        }else if(userInput.equals("2"))                             //if user input = 2, 2D-Mesh topology config will be loaded
        {
            configFile= masterFolder+"/MeshConfig.txt";
        }
        else                                                        //system will exit for any input rather than 1 or 2
        {
            System.out.println("Wrong entry ..!");
            System.out.println("Exiting...");
            System.exit(0);                                  //Exit the program...!
        }
        /////////////////////////////////////////////////////////////////////////////
        System.out.println("Set up your peer, Enter Peer ID:");
        uIn = new Scanner(System.in);
        String input=uIn.nextLine();
        readConfig(input);                                          //peer ID will be sent to the config method to load its config
        folder+="/"+input+"/";                                      //each peer has different files folder inside the main folder
        System.out.println("Your files are located in:"+folder);
        /////////////////////////////////////////////////////////////////////////////
        Thread thread1,thread2;                                     //define thread
        System.out.println("\nWaiting for peers to download files..");
        System.out.println("=======================================================\n");
        /////////////////////////////////////////////////////////////////////////////
        try
        {
            thread1 = new Thread (new Main(Integer.parseInt(peerPort))); //initiate listener thread
            thread1.start();                                             //start listener
            thread2 = new Thread (new Main(Integer.parseInt(peerPort))); //initiate listener thread
            thread2.start();
        } catch(Exception e){                                            //track general errors
            e.printStackTrace();
        }
        /////////////////////////////////////////////////////////////////////////////
        File_Handler fh = new File_Handler();                           //define and initiate handler object from the main thread
        /////////////////////////////////////////////////////////////////////////////
        while (true)
        {
            //Printing the available services
            System.out.println("*********************************************************************************************");
            System.out.println("Type the action number as following:");
            System.out.println("1. Search a file over the topology.");
            System.out.println("2. Register a file on your local machine.");
            System.out.println("3. Register all files of the working directory.");
            System.out.println("4. Download file from a peer.");
            System.out.println("5. List my files of the current directory.");
            System.out.println("6. Calculate the performance of search requests.");
            System.out.println("7. To exit.");
            System.out.println("*********************************************************************************************\n");
            Scanner in = new Scanner(System.in);
            userInput = in.nextLine();                         //get the chosen service from the user
            /////////////////////////////////////////////////////////////////////////////
            if (userInput.equals("1"))                         //if user input is 1
            {
                System.out.println("Enter the file name along with the file extension");
                userInput = in.nextLine();                     //get file name that user want to search
                if(fh.isValidName(userInput))                  //local validity of file name to enhance the performance
                    fh.Search_for_a_File(userInput);           //call search function and attach the file name
                else
                    System.out.println("Wrong file format !"); //wrong file name format
            }
            /////////////////////////////////////////////////////////////////////////////
            else if (userInput.equals("2"))                    //if user entered 2
            {
                System.out.println("Enter the file name along with the file extension");
                userInput = in.nextLine();                     // get file name that user want to register
                if(fh.isValidName(userInput))                  //local validity of file name to enhance the performance
                    fh.Register_a_File(userInput);             //call register function and attach the file name
                else
                    System.out.println("Wrong file format !"); //wrong file name format
            }
            /////////////////////////////////////////////////////////////////////////////
            else if (userInput.equals("3"))                    //if user entered 3
            {
                register_all_files(fh);                        //call register all files function
            }
            /////////////////////////////////////////////////////////////////////////////
            else if (userInput.equals("4"))                    //if user entered 4
            {

                fh.Download_From_Peer();                       //call downloading function
            }
            /////////////////////////////////////////////////////////////////////////////
            else if (userInput.equals("5"))                    //if user entered 5
            {
                list_my_files();                               //exit the program
            }
            /////////////////////////////////////////////////////////////////////////////
            else if (userInput.equals("6"))                    //if user entered 6
            {
                System.out.println("Enter the file name along with the file extension");
                userInput = in.nextLine();                     // get file name that user want to register
                if(fh.isValidName(userInput)) {
                    System.out.println("Enter the required number of requests");
                    int loop ;                                  // get file name that user want to register
                    if(in.hasNextInt()){
                        loop = in.nextInt();
                        System.out.println("Test time: "+System.currentTimeMillis());
                        System.out.println(loop+" search requests average rate is " + get_performance_measurement_for_search_request(fh, userInput.trim(),loop) + " ms.");
                    }else{
                        System.out.println("Wrong integer format !");
                    }

                }
                else
                    System.out.println("Wrong file format !"); //wrong file name format
            }
            /////////////////////////////////////////////////////////////////////////////
            else if (userInput.equals("7"))                    //if user entered 6
            {
                System.out.println("Exiting...");
                System.exit(0);                         //exit the program
            }
            else
            {
                // awareness for the user of the correct options
                System.out.println("Wrong input! the input should be 1, 2, 3, or 4 ..\n");
            }
        }
    }

    /*********************************************************************************************/

    public synchronized void run() //listening thread
    {

        if (flag == false) {                                // this flag is used to indicate the thread type
            try {
                flag=true;
                check_files_validity();                     //call check files validity
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        /////////////////////////////////////////////////////////////////////////////
        else {
            try {
                ServerSocket ssock = new ServerSocket(port); //initiate a socket that listen to the specified port
                while (true) {                               //keep listening
                    Socket sock = null;
                    sock = ssock.accept();                   //accept peer connection
                    new Listener(sock, port).start();        //create a new thread for every new connection
                }
            } catch (UnknownHostException unknownHost) {     //To Handle Unknown Host Exception
                System.err.println("host not available..!");
            } catch (IOException ioException) {              //To Handle Input-Output Exception
                ioException.printStackTrace();
            }
        }
    }

    /*********************************************************************************************/
    //list all the files in the resources folder
    public static void list_my_files()
    {
        try {
            File f = new File(folder);                       //get the resources folder path
            File[] listOfFiles = f.listFiles();              //store files into file array
            if (!listOfFiles.equals(null)) {                 //if folder is not empty
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains("txt"))//make sure it is a file and .txt
                        System.out.println("File: " + listOfFiles[i].getName());            //print list of files
                }
            }
        }catch (Exception e)
        {
            System.out.print(e.toString());
        }
    }

    /*********************************************************************************************/

    //register all the files from the resources folder
    public static void register_all_files(File_Handler fh) {
        try {
            File f = new File(folder);                                                      //get the resources folder path
            File[] listOfFiles = f.listFiles();                                             //store files into file array
            if (!listOfFiles.equals(null)) {                                                //if folder is not empty
                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains("txt"))//make sure it is a file and .txt
                        fh.Register_a_File(listOfFiles[i].getName());                       //print list of files
                }
            }
        }catch (Exception e)
        {
            System.out.print(e.toString());
        }
    }

    static long get_performance_measurement_for_search_request(File_Handler f, String fileName , int loop)
    {
        long sum=0;
        try
        {
            System.out.println("Start calculating.........!");
            for(int i=0 ;i<loop;i++)
            {
                long startTime = System.currentTimeMillis();    //store the current time
                f.Search_for_a_File(fileName);
                sum=sum+System.currentTimeMillis() - startTime; // time needed for the packet to return form the server
                System.out.println("End calculating.........!");
            }
            sum=sum/loop;                                       //calculate the average
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return sum;
    }

    //read the config file which explain the design of the topology to know your neighbours
    public static void readConfig(String userPeerID)
    {
        try {
            File f = new File(configFile);                          //load the config file based on the user input
            FileReader reader = new FileReader(f);                  //read config file
            BufferedReader br = new BufferedReader(reader);
            String value=new String();
            List<String> lines = new ArrayList();                   //define list of config lines
            String [] line;
            List<String> neighbors = new ArrayList();               //define a list for the neighbors
            int counter=0;
            /////////////////////////////////////////////////////////////////////////////
            while((value=br.readLine())!=null)     //Appending the content read from the BufferedReader object until it is null and stores it in str
            {
                lines.add(counter++,value);
                String [] valueArray=value.split(" ");       //split by space
                line=value.split(" ");                       //split peer config information (id, ip, and port)
                if(valueArray[0].equals(userPeerID))
                {
                    peerID=line[0];                                //store split result into peerID
                    peerIP=line[1];                                //store split result into peerIP
                    peerPort=line[2];                              //store split result into peerPort
                    Collections.addAll(neighbors, line[3].split("-"));
                    System.out.print(peerID+" "+peerIP+" "+Integer.parseInt(peerPort)+"\n");
                }
                peersList.add(new Neighbor(line[0],line[1],Integer.parseInt(line[2]))); //add the result to peer list
            }
            /////////////////////////////////////////////////////////////////////////////
            System.out.print("Your neighbors are:\n");
            for(int i=0; i<neighbors.size();i++)                  //looping the neighbours list
            {
                String []result ;
                String []result2 ;
                for(int j=0;j<lines.size();j++)
                {
                    result= lines.get(j).split(" ");
                    result2= neighbors.get(i).split(" ");
                    if(result2[0].trim().equals(result[0].trim()))
                    {
                        neighborList.add(i,new Neighbor(result[0],result[1],Integer.parseInt(result[2]))); //add neighbors to the list
                        System.out.print(result[0]+" "+result[1]+" "+Integer.parseInt(result[2])+"\n");
                    }
                }
            }
            /////////////////////////////////////////////////////////////////////////////
            br.close();
            reader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    //this method used to keep track on the files changes, and it runs every 10000 msec
    public void check_files_validity() throws InterruptedException {
        while(true) {
            try {
                System.out.println("Checking local files validity !\n");
                File f = new File(folder);                                                      //get the resources folder path
                File[] listOfFiles = f.listFiles();                                             //store files into file array
                if (!listOfFiles.equals(null)) {                                                //if folder is not empty
                    for (int i = 0; i < listOfFiles.length; i++) {
                        if (listOfFiles[i].isFile() && listOfFiles[i].getName().contains("txt"))//make sure it is a file and .txt
                            for (int j = 0; j < filesWatchers.size(); j++) {
                                if (listOfFiles[i].getName().trim().equals(filesWatchers.get(j).file.getName().trim())) {
                                    if (listOfFiles[i].lastModified() != filesWatchers.get(j).lastUpdate) {  //check the validity of the file
                                        System.out.println("Invalid file copy has been detected ! "+listOfFiles[i].getName()+"\n");
                                        filesWatchers.get(j).setLstUpdate(listOfFiles[i].lastModified());
                                        filesWatchers.get(j).setVersion();
                                        new File_Handler().push(listOfFiles[i].getName().trim(),filesWatchers.get(j).fileVersionNumber); //call push method and pass the version number
                                    }
                                }
                            }
                    }
                }
            } catch (Exception e) {
                System.out.print(e.toString());
            }
            sleep(20000);                                                                   //sleep for 20 seconds
        }
    }
}




