public class FileSystem {
    private Directory dir; // the root directory
    private SuperBlock superblock;
    private FileTable filetable;

    public FileSystem(int diskBlocks) {
        superblock = new SuperBlock(diskBlocks);
        dir = new Directory(superblock.totalInodes);
        filetable = new FileTable(dir);

        // read the "/" file from disk
        FileTableEntry dirEnt = this.open("/", "r");
        int dirSize = fsize(dirEnt);
        if (dirSize > 0) {
            // directory has some data
            byte[] dirData = new byte[dirSize];
            read(dirEnt, dirData);
            dir.bytes2directory(dirData);
        }
        close(dirEnt);
    }

    public int read(FileTableEntry ftEnt, byte[] buffer) { // read from file
        if (ftEnt == null || (ftEnt.mode == "a") || (ftEnt.mode == "w")) { // check if file is open for reading
            return -1;
        }
        int bytes = 0; 
        int length = buffer.length;
        int fileSize = fsize(ftEnt);
        synchronized (ftEnt) {
            while (ftEnt.seekPtr < fileSize && length > 0 ) { // while there is still data to read
                int blockNum = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (blockNum == -1) {
                    return bytes;
                }
                byte[] block = new byte[Disk.blockSize];
                SysLib.rawread(blockNum, block);
                int offset = ftEnt.seekPtr % Disk.blockSize;
                int blockReadLength = Disk.blockSize - offset;
                int fileReadLength = fileSize - ftEnt.seekPtr;
                int read = Math.min(Math.min(blockReadLength, length), fileReadLength);
                System.arraycopy(block, offset, buffer, bytes, read);
                length -= read;
                ftEnt.seekPtr += read;
                bytes += read;
                
            }
            return bytes;
        }
    }

    boolean format(final int n) {
        superblock.format(n);
        dir = new Directory(superblock.totalInodes);
        filetable = new FileTable(dir);
        return true;
    }
   
    public int write(FileTableEntry ftEnt, byte[] buffer) { // write buffer to file
        if (ftEnt.mode == "r") { // check if file is open for writing
            return -1;
        }
        synchronized (ftEnt) { // synchronize file table entry
            int bytes = 0;
            int length = buffer.length;
            while (length > 0) { // while there is still data to write
                int Block = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (Block == -1) {
                    short newPoint = (short) superblock.getFreeBlock();
                    int point = ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, newPoint);
                    if (point == -1 || point == -2) {
                        return -1;
                    } else if (point == -3) {
                        short free = (short) superblock.getFreeBlock();
                        if (!ftEnt.inode.registerIndexBlock(free)) {
                            return -1;
                        }
                        if (ftEnt.inode.registerTargetBlock(ftEnt.seekPtr, newPoint) != 0) {
                            return -1;
                        }
                    }
                    Block = newPoint;
                }
                byte[] block = new byte[Disk.blockSize]; // create a new array
                if (SysLib.rawread(Block, block) == -1) {
                    System.exit(2);
                } // read block from disk
                int offset = ftEnt.seekPtr % Disk.blockSize;
                int write = Math.min(Disk.blockSize - offset, length);
                System.arraycopy(buffer, bytes, block, offset, write);
                ftEnt.seekPtr += write;
                bytes += write;
                length -= write;
                if (ftEnt.seekPtr > ftEnt.inode.length) {
                    ftEnt.inode.length = ftEnt.seekPtr;
                }
            }
            ftEnt.inode.toDisk(ftEnt.iNumber);
            return bytes;
        }

    }

    public int fsize(FileTableEntry ftEnt) {
        synchronized (ftEnt) {
            return ftEnt.inode.length;
        }
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        if (ftEnt.inode.count != 1 || ftEnt == null) {
            return false;
        }
        byte[] releasedBlocks = ftEnt.inode.unregisterIndexBlock();
        if (releasedBlocks != null) {
            int num = SysLib.bytes2short(releasedBlocks, 0);
            while (num != -1) {
                superblock.returnBlock(num);
            }
        }
        for (int i = 0; i < Inode.directSize; i++)
            if (ftEnt.inode.direct[i] != -1) {
                superblock.returnBlock(ftEnt.inode.direct[i]);
                ftEnt.inode.direct[i] = -1;
            }
            ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

    public FileTableEntry open(String filename, String mode) { // open file
        FileTableEntry ftEnt = filetable.falloc(filename, mode);
        if (ftEnt == null) {
            return null;
        }
        if (mode == "w" && this.deallocAllBlocks(ftEnt) == false) {
            return null;
        }
        return ftEnt;
    }

    public boolean close(FileTableEntry ftEnt) { // close file
        synchronized (ftEnt) { // synchronize file table entry
            ftEnt.count--;
            if (ftEnt.count > 0) {
                return true;
            }
        }
        return filetable.ffree(ftEnt);
    }

    public boolean delete(String filename) { // delete file
        FileTableEntry open = open(filename, "w"); // open file
        short iNumber = open.iNumber;
        return close(open) && dir.ifree(iNumber);
    }

    int seek(FileTableEntry ftEnt, int offset, int whence) { // seek to offset
        synchronized (ftEnt) {
            switch(whence) { // check whence
				case 0:
                ftEnt.seekPtr = offset;
					break;
				case 1:
                ftEnt.seekPtr += offset;
					break;
				case 2:
                ftEnt.seekPtr = ftEnt.inode.length + offset;
					break;
				default:
					return -1;
			}
			if (ftEnt.seekPtr < 0) {
				ftEnt.seekPtr = 0;
			}
			if (ftEnt.seekPtr > ftEnt.inode.length) {
				ftEnt.seekPtr = ftEnt.inode.length;
			}
			return ftEnt.seekPtr;
		}
    }

}
