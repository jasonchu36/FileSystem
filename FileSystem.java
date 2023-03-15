public class FileSystem {
    private Vector table; // the actual entity of this file table
    private Directory dir; // the root directory
    private SuperBlock superblock;
    private FileTable filetable;

    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        dir = new Directory(superblock.totalInodes);
        filetable = new FileTable(dir);

        // read the "/" file from disk
        FileTableEntry dirEnt = open("/", "r");
        int dirSize = fsize(dirEnt);

        if (dirSize > 0) {
            // directory has some data
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            dir.bytes2directory(dirData);
        }
        close(dirEnt);
    }
    
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
                if (mode.compareTo("r") == 0) {
                    if (inode.flag == 0) { // == read. nothing needs to happen
                        break;
                    } else if (inode.flag == ) // to be deleted 
                    {
                        iNumber = -1;
                        return null;
                    }
                    {
                        try{
                            wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                } else if (mode.compareTo( "w" )) {
                    // do something 
                    
                }
            }

        }
        inode.count++;
        inode.toDisk(iNumber);
        FileTableEntry e = new FileTableEntry( inode,iNumber,mode);
        tables.addElement(e);
        return e;
    }
    public synchronized boolean ffree( FileTableEntry e ) {
        // receive a file table entry reference
        // save the corresponding inode to the disk
        // free this file table entry.
        // return true if this file table entry found in my table
    }
    public synchronized boolean fempty( ) {
        // return if table is empty
        return table.isEmpty( ); 
    } // should be called before starting a format

    public int read(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt.mode.compareTo("w") == 0 || ftEnt.mode.compareTo("a") == 0) {
            return -1;
        }
        
    }

    public int write(FileTableEntry ftEnt, byte[] buffer) {

    }

    public int fsize(FileTableEntry ftEnt) {
        assert (ftEnt != null);
        return ftEnt.inode.length;
    }

    public boolean format (int files) {
        this.superblock.format(files);
        this.dir = new Directory(this.superblock.totalInodes);
        this.filetable = new FileTable(this.dir);
        return true;
    }

    public FileTableEntry open(String filename, String mode) {
        FileTableEntry ftEnt = this.filetable.falloc(filename, mode);
        if (ftEnt == null) {
            return null;
        }
        if (mode.compareTo("w") == 0) {
            if (this.deallocAllBlocks(ftEnt) == false) {
                return null;
            }
        }
        return ftEnt;
    }

    public boolean close(FileTableEntry ftEnt) {
        synchronized (ftEnt) {
            ftEnt.count--;
            if (ftEnt.count > 0) {
                return true;
            }
        }
        return this.filetable.ffree(ftEnt);
    }


}
