import java.util.*;

public class FileTable {
    private Vector table; // the actual entity of this file table
    private Directory dir; // the root directory
    public FileTable( Directory directory ) { // constructor
    table = new Vector( ); // instantiate a file (structure) table
    dir = directory; // receive a reference to the Director
    } // from the file system
    // major public methods
    public synchronized FileTableEntry falloc( String filename, String mode ) {
    // allocate a new file (structure) table entry for this file name
    // allocate/retrieve and register the corresponding inode using dir
    // increment this inode's count
    // immediately write back this inode to the disk
    // return a reference to this file (structure) table entry
        short iNumber = -1;
        Inode inode = null;

        while (true){
            iNumber = (fnames.equal("/") ? 0 : dir.namei(filename));
            if (iNumber >= 0 ) {
                inode = new Inode (iNumber);
                if (mode.compareTo("r")) {
                    if (inode.flag == 2) { // == read. nothing needs to happen
                        break;
                    } else if (inode.flag == 3) { // == write
                        try{
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else if (inode.flag == 4) // to be deleted
                    {
                       iNumber = -1;
                       return null; 
                    }
                } else if (mode.compareTo( "w" )) {
                    iNumber = directory.iAlloc(filename);
                    inode = new Inode(iNumber);
                    iNode.Flag = 2;
                    break;
                }
            }
        }

        inode.count++;
        inode.toDisk(iNumber);
        FileTableEntry e = new FileTableEntry ( inode, iNumber, mode );
        table.addElement(e);
        return e;
    }
    public synchronized boolean ffree( FileTableEntry e ) {
    // receive a file table entry reference
    // save the corresponding inode to the disk
    // free this file table entry.
    // return true if this file table entry found in my table
        if (table.removeElement(e)) {
            Inode inode = e.inode;
            --inode.count;
            switch(e.inode.flag) {
                case 1: // read
                    e.inode.flag = 0;
                    break;
                case 2: // write
                    e.inode.flag = 0;
                    break;
                case 3: // read/write
                    e.inode.flag = 3;
                    break;
                case 4: // delete
                    e.inode.flag = 3;
                    break;}
            
            e.inode.toDisk(e.iNumber);
            notify();
            return true;
        }
        
    }
    public synchronized boolean fempty( ) {
        // return if table is empty
        return table.isEmpty( ); 
        } // should be called before starting a format
    }