/*
 * This is a template of SyncQueue.java. Chagne this file name into SyncQueue.java and
 * complete the implementation
 */
public class SyncQueue {
    private QueueNode queue[] = null;
    private final int COND_MAX = 10;
    private final int NO_TID = -1;

    public SyncQueue( ) {
	// You need to implement this constructor.
	// Assume SyncQueue( COND_MAX );
        this.queue = new QueueNode[COND_MAX];
        for (int i = 0; i < COND_MAX; i++) {
            queue[i] = new QueueNode();
        }
    }

    public SyncQueue( int condMax ) {
	// You need to implement this constructor.
        this.queue = new QueueNode[condMax];
        for (int i = 0; i < condMax; i++) {
            queue[i] = new QueueNode();
        }
    }

    int enqueueAndSleep( int condition ) {
	// Verify the correctness of condition.
	// Call the corresponding queue[ ].sleep( ).
	// Return the corresponding child thread ID.
        if (condition >= 0 && condition < queue.length) {
            return this.queue[condition].sleep();
        }
        return NO_TID;
    }

    void dequeueAndWakeup( int condition, int tid ) {
	// verify the correctness of condition.
	// Call the corresponding queue[ ].wakeup( ... );
       if (condition >= 0 && condition < queue.length) {
           this.queue[condition].wakeup(tid);
       }
    }

    void dequeueAndWakeup( int condition ) {
	// Assume tid = 0.
        this.dequeueAndWakeup(condition, 0);
    }
}
