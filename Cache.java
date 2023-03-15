import java.util.*;

public class Cache {
    private int blockSize;
    private Vector<byte[]> pages; // you may use: private byte[][] = null;
    private int victim;
	private byte[][] cache;
    private class Entry {
	public static final int INVALID = -1;
	public boolean reference;
	public boolean dirty;
	public int frame;
	
	public Entry( ) {
	    reference = false;
	    dirty = false;
	    frame = INVALID;
	}
	
    }
    private Entry[] pageTable = null;

    private int nextVictim( ) { // if errors this prob it
		while (true) {
			victim = (victim + 1) % pageTable.length;
			if (!pageTable[victim].reference) {
				break;
			}
			pageTable[victim].reference = false;
		}
		return victim;
    }

    private void writeBack( int victimEntry ) {
        if ( pageTable[victimEntry].frame != Entry.INVALID &&
             pageTable[victimEntry].dirty == true ) {
	    	SysLib.rawwrite( pageTable[victimEntry].frame, pages.elementAt(victimEntry) ); 
	    	pageTable[victimEntry].dirty = false;
	}
    }

    public Cache( int blockSize, int cacheBlocks ) {
	// instantiate pages
	// instantiate and initialize pageTable
		this.pageTable = null;
		this.blockSize = blockSize;
		this.victim = cacheBlocks -1;
		this.pages = new Vector<byte[]>();
		this.cache = new byte[cacheBlocks][blockSize];
		this.pageTable = new Entry[cacheBlocks];
		for(int i = 0; i < cacheBlocks; i++){
			this.pages.addElement(new byte[blockSize]);
			this.pageTable[i] = new Entry();
			for(int j = 0; j < blockSize; j++){
				this.pages.elementAt(i)[j] = 0;
			}
    	}
	}
	public int readHelper() {
		for (int i = 0; i < pageTable.length; i++) {
			if (pageTable[i].frame == -1) {
				return i;
			}
		}
		return -1;
	}
    public synchronized boolean read( int blockId, byte buffer[] ) {
		if ( blockId < 0 ) {
	    	SysLib.cerr( "threadOS: a wrong blockId for cread\n" );
	    	return false;
		}

	// locate a valid page
	for ( int i = 0; i < pageTable.length; i++ ) {
	    if ( pageTable[i].frame == blockId ) {
		System.arraycopy(pages.elementAt(i), 0, buffer, 0, blockSize);
		pageTable[i].reference = true;
		return true;
	    }
	}

	// page miss!!
        // find an invalid page
	// if no invalid page is found, all pages are full
	//    seek for a victim
       	int victimEntry;
		if (readHelper() != -1) {
			victimEntry = readHelper();
		} else {
			victimEntry = nextVictim();
		}
	// write back a dirty copy
		writeBack( victimEntry );
	// read a requested block from disk
		SysLib.rawread( blockId, buffer );

	// cache it
	// copy pages[victimEntry] to buffer
		byte[] temp = new byte[blockSize];
		System.arraycopy(buffer, 0, temp, 0, blockSize);
		pages.set(victimEntry, temp);
		pageTable[victimEntry].frame = blockId;
        pageTable[victimEntry].reference = true;
		return true;
    }

    public synchronized boolean write( int blockId, byte buffer[] ) {
	if ( blockId < 0 ) {
	    SysLib.cerr( "threadOS: a wrong blockId for cwrite\n" );
	    return false;
	}

	// locate a valid page
	for ( int i = 0; i < pageTable.length; i++ ) {
	    if ( pageTable[i].frame == blockId ) {
			byte[] beboop = new byte[blockSize];
			System.arraycopy(buffer, 0, beboop, 0, blockSize);
			pages.set(i, beboop);
			pageTable[i].reference = true;
        	pageTable[i].dirty = true;
			return true;
	    }
	}
	    int victimEntry;
		if (readHelper() != -1) {
			victimEntry = readHelper();
		} else {
			victimEntry = nextVictim();
		}

	// write back a dirty copy
        writeBack( victimEntry );

	// cache it but not write through.
	// copy buffer to pages[victimEntry]
		byte[] temp = new byte[blockSize];
		System.arraycopy(buffer, 0, temp, 0, blockSize);
		pages.set(victimEntry, temp);
		pageTable[victimEntry].frame = blockId;
        pageTable[victimEntry].reference = true;
        pageTable[victimEntry].dirty = true;
		return true;
    }

    public synchronized void sync( ) {
	for ( int i = 0; i < pageTable.length; i++ )
	    writeBack( i );
	SysLib.sync( );
    }

    public synchronized void flush( ) {
	for ( int i = 0; i < pageTable.length; i++ ) {
	    writeBack( i );
	    pageTable[i].reference = false;
	    pageTable[i].frame = Entry.INVALID;
	}
	SysLib.sync( );
    }
}
