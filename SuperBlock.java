import java.util.*;

class SuperBlock {
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
            return;

        } else {
            totalBlocks = diskSize;
            format(defaultInodeBlocks);
        }
    }

    void sync() {
        // write back in-memory superblock to disk: SysLib.rawwrite( 0, superblock );
        byte[] superBlock = new byte[Disk.blockSize];
        SysLib.int2bytes(totalBlocks, superBlock, 0);
        SysLib.int2bytes(totalInodes, superBlock, 4);
        SysLib.int2bytes(freeList, superBlock, 8);
        SysLib.rawwrite(0, superBlock);
        SysLib.cerr("we are in superblock sync\n");
        SysLib.cerr("Superblock syncronized\n");
    }

    void format(int files) {
        totalInodes = files;
        for (short i = 0; i < totalInodes; i++) {
            Inode node = new Inode();
            node.flag = 0;
            node.toDisk(i);
        }
        this.freeList = 2 + totalInodes * 32 / 512;
         for(int i = this.freeList; i < this.totalBlocks; i++)
        {
            byte[] Block = new byte[512];
            for(int j = 0; j < 512; j++)
            {
                Block[j] = 0;
            }
            SysLib.int2bytes(i + 1, Block, 0);
            SysLib.rawwrite(i, Block);
        }
        this.sync();
    }

    public int getFreeBlock() {
        // get a new free block from the freelist
        int freeBlockNumber = this.freeList;
        if (freeBlockNumber != -1) {
            byte[] Block = new byte[512];
            SysLib.rawread(freeBlockNumber, Block);
            this.freeList = SysLib.bytes2int(Block, 0);
            SysLib.int2bytes(0, Block, 0);
            SysLib.rawwrite(freeBlockNumber, Block);
        }
        return freeBlockNumber;
    }

    public boolean returnBlock( int oldBlockNumber ) {
        // return this old block to the free list. The list can be a stack.
        byte[] Block = new byte[512];
        if(oldBlockNumber >= 0) {
            for (int i = 0; i < 512; i++) {
                Block[i] = 0;
            }
        }
        SysLib.int2bytes(this.freeList , Block, 0);
        SysLib.rawwrite(oldBlockNumber, Block);
        this.freeList = oldBlockNumber;
        return false;
    }
}
