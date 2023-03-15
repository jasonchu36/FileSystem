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
