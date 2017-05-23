/**
 * Created by kmursi on 4/8/17.
 * this class define the neighbor structure
 */
public class Neighbor {
    String neighborID;
    String neighborIP;
    int neighborPort;

    public Neighbor(String neighborID, String neighborIP, int neighborPort)
    {
        this.neighborID=neighborID;                         //peer ID
        this.neighborIP=neighborIP;                         //IP address from config file
        this.neighborPort=neighborPort;                     //Port number from config file

    }
}
