public class FileSystem {
    private Vector table; // the actual entity of this file table
    private Directory dir; // the root directory
    private SuperBlock superblock; // the super block
  	private FileTable filetable;    // the file table
    public FileTable( Directory directory ) { // constructor
        table = new Vector( ); // instantiate a file (structure) table
        dir = directory; // receive a reference to the Director
    } // from the file system
    // major public methods
    public void sync() {
        FileTableEntry dirEntry = open ("/", "w");
        byte[] data = dir.directory2bytes();
        write(dirEntry, data);
        close(dirEntry);
        superblock.sync();
    }
}
