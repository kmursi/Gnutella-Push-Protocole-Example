/**
 * Created by kmursi on 4/9/17.
 * this class keep track the files update
 */
import java.io.File;
public class FileLastUpdate {
    File file;
    long lastUpdate;
    int fileVersionNumber;
    public FileLastUpdate(File file, long lastUpdate,int fileVersionNumber)
    {
        this.file=file;                                                       //file name
        this.lastUpdate=lastUpdate;                                           //last update
        this.fileVersionNumber=fileVersionNumber;                             //version number
    }

    public void setLstUpdate(long updateTime)
    {
        this.lastUpdate=updateTime;
    } //set update time
    public void setVersion()                                                  // increase version number
    {
        this.fileVersionNumber++;
    }
}
