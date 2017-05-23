

/**
 * Created by kmursi on 3/11/17.
 * this class define the message structure
 */
import java.io.Serializable;
public class Message implements Serializable{
    String id;
    String ip;
    String command;
    String data;
    int TTL=0;
    int port;
    String content;
    String header;
    String PK;
    int fileVersionNumber=0;
    public Message(String id, String ip, String command, String data,int TTL, int port, String content, String header)
    {
        this.id=id;                                             //message ID
        this.command=command;                                   //type of the piggybacked command
        this.ip=ip;                                             //sender IP address
        this.port=port;                                         //sender port
        this.data=data;                                         //message date
        this.TTL=TTL;                                           //message time-to-live
        this.content=content;                                   //message content if needed along with data
        this.header=header;                                     //message header keeps track the path of bypass peers
    }
    public void set_TTL(String sign)
    {
        if(sign.equals("+"))                                    //increment TTL
            TTL++;
        else if (sign.equals("-"))                              //decrement TTL
            TTL--;
    }

    public void append_header(String id)
    {
        header=header+"-"+id;
    }//append the peer id of the current peer

    public void cut_header()
    {
        String [] content= header.split("-");               //cut one of the header when forward message back
        header=null;
        for(int i=0;i<content.length-1;i++)
        {
            header=header+"-"+content[i];
        }
    }
    public void set_PK()                                           //set message primary key to identify the message
    {
        PK=Main.peerID+System.currentTimeMillis();
    }
    public void set_file_version_number(int version)
    {
        fileVersionNumber+=version;
    } //set version number to a file
}
