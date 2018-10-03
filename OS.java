import java.util.*;
import java.util.concurrent.*;
import java.io.*;

/*
 * Current scheduling policy is shortest process next
 */
public class OS {
    private final int QUEUE_CAPACITY = 25; //sets limit for queue
    private final int NUM_PROCESSES = 8; //so main mem size = 32
    private final String POLICY = "LRU";
    
    private int quantumCount; //for rr scheduling policy
    private Queue<Process> newQueue;
    private Queue<Process> readyQueue;
    private Queue<Process> readySuspendedQueue;
    private Queue<Process> blockedQueue;
    private Queue<Process> blockedSuspendedQueue;
    private Queue<Process> exitQueue;

    private Generator g;

    public OS(){
	GlobalVariables.mm = new MemoryManager(NUM_PROCESSES, POLICY);
	
	g = new Generator(GlobalVariables.BATCH_TIME, GlobalVariables.AVG_INTR_ARRIVAL);
	g.generateNewList();

	newQueue = new ArrayBlockingQueue<Process>(QUEUE_CAPACITY);
	exitQueue = new ArrayBlockingQueue<Process>(QUEUE_CAPACITY);

	quantumCount = 0;
	
	switch(GlobalVariables.SCHED_POLICY){
	case(GlobalVariables.SRT):
	    readyQueue = new PriorityBlockingQueue<Process>();
	    readySuspendedQueue = new PriorityBlockingQueue<Process>();
	    blockedQueue = new PriorityBlockingQueue<Process>();
	    blockedSuspendedQueue = new PriorityBlockingQueue<Process>();
	    break;
	default:
	    readyQueue = new ArrayBlockingQueue<Process>(QUEUE_CAPACITY);
	    readySuspendedQueue = new ArrayBlockingQueue<Process>(QUEUE_CAPACITY);
	    blockedQueue = new ArrayBlockingQueue<Process>(QUEUE_CAPACITY);
	    blockedSuspendedQueue = new ArrayBlockingQueue<Process>(QUEUE_CAPACITY);
	    break;
	}

    }

    public void makeReadyFromNew(Process p){
	if(GlobalVariables.mm.roomInMainMem()){
	    GlobalVariables.write("\tARRIVING: Process " + p.getId() + " placed in mainmem");
	    p.setStatus(GlobalVariables.READY_STATUS);
	    readyQueue.offer(p);
	    GlobalVariables.mm.addProcessToMain(p);
	}
	else{
	    GlobalVariables.write("\tARRIVING: Process " + p.getId() + " suspended");
	    p.setStatus(GlobalVariables.READY_SUSPENDED_STATUS);
	    readySuspendedQueue.offer(p);
	    checkForSuspendChanges();
	}
    }

    public void run(int runTime){
	switch(GlobalVariables.SCHED_POLICY){
	case GlobalVariables.FCFS:
	    GlobalVariables.write("\nScheduling algorithm: First Come First Serve");
	    while(GlobalVariables.time < (runTime+16)){
		GlobalVariables.write("Time: " + GlobalVariables.time);
		fcfsScheduler();
		GlobalVariables.time++;
	    }
	    break;
	case GlobalVariables.SRT:
	    GlobalVariables.write("\nScheduling algorithm: Shortest Remaining Time");
	    while(GlobalVariables.time < (runTime+16)){
		GlobalVariables.write("Time: " + GlobalVariables.time);
		srtScheduler();
		GlobalVariables.time++;
	    }
	    break;
	case GlobalVariables.RR:
	    GlobalVariables.write("\nScheduling algorithm: Round Robin");
	    while(GlobalVariables.time < (runTime+16)){
		GlobalVariables.write("Time: " + GlobalVariables.time);
		rrScheduler();
		GlobalVariables.time++;
	    }
	    break;
	default:
	    GlobalVariables.write("\nScheduling algorithm: First Come First Serve");
	    while(GlobalVariables.time < (runTime+16)){
		GlobalVariables.write("Time: " + GlobalVariables.time);
		fcfsScheduler();
		GlobalVariables.time++;
	    }
	    break;
	}
	try {
	    GlobalVariables.writer.close();
	} catch (IOException ex) {
	    System.out.println(ex);
	}
    }

    public void run(){
	run(GlobalVariables.BATCH_TIME);
    }

    //simulates one unit of scheduler
    //this is first come first serve
    private void fcfsScheduler(){
	// check for and add arriving processes
	// makes sure completed processes aren't added by checking remaining time
	if((GlobalVariables.processes.size() != 0) && (GlobalVariables.processes.get(0).getArrivalTime() == GlobalVariables.time) && GlobalVariables.processes.get(0).getRemainingTime() > 0){
	    //add process pages to page table and disk
	    Process p = GlobalVariables.processes.get(0);
	    GlobalVariables.mm.addPagesToTableAndDisk(p);
	    GlobalVariables.processes.remove(0);
	    makeReadyFromNew(p);
	}

	//handling blocked processes that are ready to be unblocked
	if (!blockedQueue.isEmpty()){
	    Process head = blockedQueue.peek();
	    int headPID = head.getId();
	    int id = headPID;
	    do {
		Process curProcess = blockedQueue.poll();
		curProcess.decrementBlockedTime();
		if (curProcess.getBlockedTime() == 0){
		    readyQueue.offer(curProcess);
		    curProcess.setStatus(GlobalVariables.READY_STATUS);
		    GlobalVariables.write("\tUNBLOCKING: Blocked Process " + curProcess.getId());
		} else{
		    blockedQueue.offer(curProcess);
		}
		Process nextProcess = blockedQueue.peek();
		if (nextProcess != null){
		    id = nextProcess.getId();
		} 
	    } while (id != headPID && !blockedQueue.isEmpty());
	}

	//handling blocked suspended processes that are ready to be unblocked
	if (!blockedSuspendedQueue.isEmpty()){
	    Process head = blockedSuspendedQueue.peek();
	    int headPID = head.getId();
	    int id = headPID;
	    do {
		Process curProcess = blockedSuspendedQueue.poll();
		curProcess.decrementBlockedTime();
		if (curProcess.getBlockedTime() == 0){
		    readySuspendedQueue.offer(curProcess);
		    curProcess.setStatus(GlobalVariables.READY_SUSPENDED_STATUS);
		    GlobalVariables.write("\tUNBLOCKING: Blocked/Suspended Process " + curProcess.getId());
		} else{
		    blockedSuspendedQueue.offer(curProcess);
		}
		Process nextProcess = blockedSuspendedQueue.peek();
		if (nextProcess != null){
		    id = nextProcess.getId();
		} 
	    } while (id != headPID && !blockedSuspendedQueue.isEmpty());
	}
	
	if (GlobalVariables.runningProcess == null && !readyQueue.isEmpty()){
	    GlobalVariables.runningProcess = readyQueue.poll(); 
	}
	// if there is a process running, run process one unit
	// we are letting a started process run on the same unit as it is being chosen
	// this assumes a low overhead as we talked about in class
	if (GlobalVariables.runningProcess != null){
	    char c = GlobalVariables.runningProcess.runOneUnit();

	    switch (c) {
	    case '*':
		GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
		blockRunningProcess();
		break;
	    case '!':
		GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
		exitRunningProcess();
		break;
	    default:
	        GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
	    }
	}
    }

    
    private void srtScheduler(){	
	// check for and add arriving processes
	// makes sure completed processes aren't added by checking remaining time
	if((GlobalVariables.processes.size() != 0) && (GlobalVariables.processes.get(0).getArrivalTime() == GlobalVariables.time) && GlobalVariables.processes.get(0).getRemainingTime() > 0){
	    //add process pages to page table and disk
	    Process p = GlobalVariables.processes.get(0);
	    GlobalVariables.mm.addPagesToTableAndDisk(p);
	    GlobalVariables.processes.remove(0);
	    makeReadyFromNew(p);
	    if (GlobalVariables.runningProcess != null){
		readyQueue.offer(GlobalVariables.runningProcess);
		GlobalVariables.runningProcess.setStatus(GlobalVariables.READY_STATUS);
		GlobalVariables.runningProcess = null;
	    }
	}

	//handling blocked processes that are ready to be unblocked
	if (!blockedQueue.isEmpty()){
	    Process head = blockedQueue.peek();
	    int headPID = head.getId();
	    int id = headPID;
	    do {
		Process curProcess = blockedQueue.poll();
		curProcess.decrementBlockedTime();
		if (curProcess.getBlockedTime() == 0){
		    readyQueue.offer(curProcess);
		    curProcess.setStatus(GlobalVariables.READY_STATUS);
		    GlobalVariables.write("\tUNBLOCKING: Blocked Process " + curProcess.getId());
		} else{
		    blockedQueue.offer(curProcess);
		}
		Process nextProcess = blockedQueue.peek();
		if (nextProcess != null){
		    id = nextProcess.getId();
		} 
	    } while (id != headPID && !blockedQueue.isEmpty());
	}

	//handling blocked suspended processes that are ready to be unblocked
	if (!blockedSuspendedQueue.isEmpty()){
	    Process head = blockedSuspendedQueue.peek();
	    int headPID = head.getId();
	    int id = headPID;
	    do {
		Process curProcess = blockedSuspendedQueue.poll();
		curProcess.decrementBlockedTime();
		if (curProcess.getBlockedTime() == 0){
		    readySuspendedQueue.offer(curProcess);
		    curProcess.setStatus(GlobalVariables.READY_SUSPENDED_STATUS);
		    GlobalVariables.write("\tUNBLOCKING: Blocked/Suspended Process " + curProcess.getId());
		} else{
		    blockedSuspendedQueue.offer(curProcess);
		}
		Process nextProcess = blockedSuspendedQueue.peek();
		if (nextProcess != null){
		    id = nextProcess.getId();
		} 
	    } while (id != headPID && !blockedSuspendedQueue.isEmpty());
	}


	
	if (GlobalVariables.runningProcess == null && !readyQueue.isEmpty()){
	    GlobalVariables.runningProcess = readyQueue.poll();
	    GlobalVariables.runningProcess.setStatus(GlobalVariables.RUNNING_STATUS);
	}
	// if there is a process running, run process one unit
	// we are letting a started process run on the same unit as it is being chosen
	// this assumes a low overhead as we talked about in class
	if (GlobalVariables.runningProcess != null){
	    char c = GlobalVariables.runningProcess.runOneUnit();

	    switch (c) {
	    case '*':
		GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
		blockRunningProcess();
		break;
	    case '!':
		GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
		exitRunningProcess();
		break;
	    default:
	        GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
	    }
	}
    }

    private void rrScheduler(){
	// check for and add arriving processes
	// makes sure completed processes aren't added by checking remaining time
	if((GlobalVariables.processes.size() != 0) && (GlobalVariables.processes.get(0).getArrivalTime() == GlobalVariables.time) && GlobalVariables.processes.get(0).getRemainingTime() > 0){
	    //add process pages to page table and disk
	    Process p = GlobalVariables.processes.get(0);
	    GlobalVariables.mm.addPagesToTableAndDisk(p);
	    GlobalVariables.processes.remove(0);
	    makeReadyFromNew(p);
	    GlobalVariables.write(p.getPagesString());
	}

	//handling blocked processes that are ready to be unblocked
	if (!blockedQueue.isEmpty()){
	    Process head = blockedQueue.peek();
	    int headPID = head.getId();
	    int id = headPID;
	    do {
		Process curProcess = blockedQueue.poll();
		curProcess.decrementBlockedTime();
		if (curProcess.getBlockedTime() == 0){
		    readyQueue.offer(curProcess);
		    curProcess.setStatus(GlobalVariables.READY_STATUS);
		    GlobalVariables.write("\tUNBLOCKING: Blocked Process " + curProcess.getId());
		} else{
		    blockedQueue.offer(curProcess);
		}
		Process nextProcess = blockedQueue.peek();
		if (nextProcess != null){
		    id = nextProcess.getId();
		} 
	    } while (id != headPID && !blockedQueue.isEmpty());
	}

	//handling blocked suspended processes that are ready to be unblocked
	if (!blockedSuspendedQueue.isEmpty()){
	    Process head = blockedSuspendedQueue.peek();
	    int headPID = head.getId();
	    int id = headPID;
	    do {
		Process curProcess = blockedSuspendedQueue.poll();
		curProcess.decrementBlockedTime();
		if (curProcess.getBlockedTime() == 0){
		    readySuspendedQueue.offer(curProcess);
		    curProcess.setStatus(GlobalVariables.READY_SUSPENDED_STATUS);
		    GlobalVariables.write("\tUNBLOCKING: Blocked/Suspended Process " + curProcess.getId());
		} else{
		    blockedSuspendedQueue.offer(curProcess);
		}
		Process nextProcess = blockedSuspendedQueue.peek();
		if (nextProcess != null){
		    id = nextProcess.getId();
		} 
	    } while (id != headPID && !blockedSuspendedQueue.isEmpty());
	}
	
	if (GlobalVariables.runningProcess == null && !readyQueue.isEmpty()){
	    GlobalVariables.runningProcess = readyQueue.poll();
	    GlobalVariables.write("\tSTARTING: Process " + GlobalVariables.runningProcess.getId());
	}
	// if there is a process running, run process one unit
	// we are letting a started process run on the same unit as it is being chosen
	// this assumes a low overhead as we talked about in class
	if (GlobalVariables.runningProcess != null){
	    char c = GlobalVariables.runningProcess.runOneUnit();

	    switch (c) {
	    case '*':
		GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
		blockRunningProcess();
		quantumCount = 0;
		break;
	    case '!':
		GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
		exitRunningProcess();
		quantumCount = 0;
		break;
	    default:
	        GlobalVariables.write("\t" + GlobalVariables.runningProcess.getId() + ":\t" + c);
		quantumCount++;
	    }

	    if(quantumCount == GlobalVariables.ROUND_ROBIN_QUANTUM) {
		GlobalVariables.write("\tTIMEOUT: Process " + GlobalVariables.runningProcess.getId());
		readyQueue.offer(GlobalVariables.runningProcess);
		quantumCount = 0;
		GlobalVariables.runningProcess = null;
	    }
	}
    }

    private void blockRunningProcess(){
	GlobalVariables.write("\tBLOCKING: Process " + GlobalVariables.runningProcess.getId());
	blockedQueue.offer(GlobalVariables.runningProcess);
	GlobalVariables.runningProcess.setStatus(GlobalVariables.BLOCKED_STATUS);
	GlobalVariables.runningProcess.setBlockedTime(GlobalVariables.BLOCKING_TIME);
	GlobalVariables.runningProcess = null;
	checkForSuspendChanges();
    }

    private void exitRunningProcess(){
	GlobalVariables.write("\tEXITING: Process " + GlobalVariables.runningProcess.getId());
	exitQueue.offer(GlobalVariables.runningProcess);
	GlobalVariables.runningProcess.setStatus(GlobalVariables.EXIT_STATUS);
        GlobalVariables.mm.removeProcessFromMain(GlobalVariables.runningProcess);
	GlobalVariables.runningProcess = null;
	checkForSuspendChanges();
    }

    private void checkForSuspendChanges(){
	//check if there are blocked processes
	if(!blockedQueue.isEmpty() && !readySuspendedQueue.isEmpty()){
	    //suspending blocked process
	    Process suspending = GlobalVariables.removeQueueTail(blockedQueue);
	    GlobalVariables.write("\tSUSPENDING: Blocked Process " + suspending.getId());
	    blockedSuspendedQueue.offer(suspending);
	    suspending.setStatus(GlobalVariables.BLOCKED_SUSPENDED_STATUS);
	    GlobalVariables.mm.removeProcessFromMain(suspending);

	    //move ready suspended process to ready
	    Process readying = readySuspendedQueue.poll();
	    GlobalVariables.write("\tMOVING TO MAIN MEM: Suspended Process " + readying.getId());
	    readyQueue.offer(readying);
	    readying.setStatus(GlobalVariables.READY_STATUS);
	    GlobalVariables.mm.addProcessToMain(readying);
	} else if (!readySuspendedQueue.isEmpty() && GlobalVariables.mm.roomInMainMem()) {
	    //move ready suspended process to ready
	    Process readying = readySuspendedQueue.poll();
	    GlobalVariables.write("\tMOVING TO MAIN MEM: Ready/Suspended Process " + readying.getId());
	    readyQueue.offer(readying);
	    readying.setStatus(GlobalVariables.READY_STATUS);
	    GlobalVariables.mm.addProcessToMain(readying);
	}
	else if (readySuspendedQueue.isEmpty() && !blockedSuspendedQueue.isEmpty() && GlobalVariables.mm.roomInMainMem()) {
	    //move blocked suspend process to blocked
	    Process p = blockedSuspendedQueue.poll();
	    GlobalVariables.write("\tMOVING TO MAIN MEM: Blocked/Suspended Process " + p.getId());
	    blockedQueue.offer(p);
	    p.setStatus(GlobalVariables.BLOCKED_STATUS);
	    GlobalVariables.mm.addProcessToMain(p);
	}
    }
}


/*
// check for and add arriving processes; makes sure complete processes aren't added by checking remaining time
if((temp.size() != 0) && (temp.get(0).getArrivalTime() == i) && temp.get(0).getRemainingTime() > 0){
Process p = temp.remove(0);
write(writer, i + "\t\t\t|| \t" + p + "\t\t|| Arrives\n");
		
srtReadyQueue.offer(p);
if(currentRunning != null) {
//we put currentRunning back into queue, allowing priority queue to handle comparison
srtReadyQueue.offer(currentRunning); 
currentRunning = null;
}
}
*/
