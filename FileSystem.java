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
        System.out.println("dirSize");
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
        int bytes = 0;
        int length = buffer.length;
        synchronized (ftEnt) {
            while (length > 0 && ftEnt.seekPtr < fsize(ftEnt)) {
                int blockNum = ftEnt.inode.findTargetBlock(ftEnt.seekPtr);
                if (blockNum == -1) {
                    break;
                }
                byte[] block = new byte[Disk.blockSize];
                SysLib.rawread(blockNum, block);
                int offset = ftEnt.seekPtr % Disk.blockSize;
                int read = Math.min(Math.min(Disk.blockSize - offset, length), fsize(ftEnt) - ftEnt.seekPtr);
                System.arraycopy(block, offset, buffer, bytes, read);
                ftEnt.seekPtr += read;
                bytes += read;
                length -= read;
            }
            return bytes;
        }
    }

    boolean format(final int n) {
        while (!filetable.fempty()) {
        }
        superblock.format(n);
        dir = new Directory(superblock.totalInodes);
        filetable = new FileTable(dir);
        return true;
    }

    public int write(FileTableEntry ftEnt, byte[] buffer) {
        if (ftEnt.mode == "r") {
            return -1;
        }
        synchronized (ftEnt) {
            int bytes = 0;
            int length = buffer.length;
            while (length > 0) {
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
                byte[] block = new byte[Disk.blockSize];
                if (SysLib.rawread(Block, block) == -1) {
                    System.exit(2);
                }
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
        assert (ftEnt != null);
        return ftEnt.inode.length;
    }

    private boolean deallocAllBlocks(FileTableEntry ftEnt) {
        for (int i = 0; i < ftEnt.inode.direct.length; i++) {
            if (ftEnt.inode.direct[i] != -1) {
                this.superblock.returnBlock(ftEnt.inode.direct[i]);
                ftEnt.inode.direct[i] = -1;
            }
        }
        if (ftEnt.inode.indirect != -1) {
            byte[] indirectBlock = new byte[Disk.blockSize];
            SysLib.rawread(ftEnt.inode.indirect, indirectBlock);
            for (int i = 0; i < indirectBlock.length; i += 2) {
                short blockNum = SysLib.bytes2short(indirectBlock, i);
                if (blockNum != -1) {
                    this.superblock.returnBlock(blockNum);
                }
            }
            this.superblock.returnBlock(ftEnt.inode.indirect);
            ftEnt.inode.indirect = -1;
        }
        ftEnt.inode.toDisk(ftEnt.iNumber);
        return true;
    }

    public FileTableEntry open(String filename, String mode) {
        FileTableEntry ftEnt = filetable.falloc(filename, mode);
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
        return filetable.ffree(ftEnt);
    }

    public boolean delete(String filename) {
        FileTableEntry open = open(filename, "w");
        short iNumber = open.iNumber;
        return close(open) && dir.ifree(iNumber);
    }

    int seek(FileTableEntry ftEnt, int offset, int whence) {
        synchronized (ftEnt) {
            switch (whence) {
                case 0: // SEEK_SET
                    if (offset >= 0 && offset <= ftEnt.inode.length) {
                        ftEnt.seekPtr = offset;
                        break;
                    }
                    return -1;
                case 1: // SEEK_CUR
                    if (offset >= 0 && offset + ftEnt.seekPtr <= ftEnt.inode.length) {
                        ftEnt.seekPtr += offset;
                        return ftEnt.seekPtr;
                    }
                    break;
                case 2: { // SEEK_END
                    if (offset <= 0 && offset + ftEnt.inode.length >= 0) {
                        ftEnt.seekPtr = ftEnt.inode.length + offset;
                        break;
                    }
                    return -1;
                }

            }
            return ftEnt.seekPtr;
        }

    }

}
