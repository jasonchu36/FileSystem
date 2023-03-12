import java.util.*;

class Superblock {
    public int totalBlocks; // the number of disk blocks
    public int totalInodes; // the number of inodes
    public int freeList; // the block number of the free list's head
    private final int defaultInodeBlocks = 64;

    public SuperBlock( int diskSize ) {
        // read the superblock from disk.
        // check disk contents are valid.
        // if invalid, call format( ).
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.rawread(0, superBlock);
        totalBlocks = SysLib.bytes2int(superBlock, 0);
        totalInodes = SysLib.bytes2int(superBlock, 4);
        freeList = SysLib.bytes2int(superBlock, 8);
        
        if (totalBlocks == diskSize && totalInodes > 0  && freeList >= 2) {
            //return;
            
        } else {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    void sync() {
        // write back in-memory superblock to disk: SysLib.rawwrite( 0, superblock );
    }

    void format(int files) {
        // initialize the superblock
        // initialize each inode and immediately write it back to disk
        // initialize free blocks
    }

    public int getFreeBlock() {
        // get a new free block from the freelist
        return freeBlockNumber;
    }

    public boolean returnBlock( int oldBlockNumber ) {
        // return this old block to the free list. The list can be a stack.
        return true or false;
        }
}
