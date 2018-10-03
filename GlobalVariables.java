import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Queue;
import java.io.*;
public class GlobalVariables {
    public static MemoryManager mm;
    public static ArrayList<Process> processes;
    public static Process runningProcess;
    public static BufferedWriter writer;
    public static int time = 0;

    //Final Variables
    public final static int NOT_IN_MAIN_MEM = -1;
    public final static int PAGE_LENGTH = 4; //todo change to 8 when working
    public final static int BLOCKING_TIME = 4;
    public final static String NEW_STATUS = "new";
    public final static String READY_STATUS = "ready";
    public final static String READY_SUSPENDED_STATUS = "ready/suspend";
    public final static String RUNNING_STATUS = "running";
    public final static String BLOCKED_STATUS = "blocked";
    public final static String BLOCKED_SUSPENDED_STATUS = "blocked/suspend";
    public final static String EXIT_STATUS = "exited";
    public final static String PAGE_ALPHABET = "*ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public final static String FCFS = "FCFS";
    public final static String SRT = "SRT";
    public final static String RR = "RR";

    //User input
    public final static int BATCH_TIME = 32;
    public final static int AVG_INTR_ARRIVAL = 4;
    public final static int ROUND_ROBIN_QUANTUM = 6;
    public final static int RESIDENT_SET_SIZE = 4;
    public final static int MAIN_MEM_SIZE = RESIDENT_SET_SIZE*2;
    public final static String SCHED_POLICY = SRT; //FCFS | SRT | RR

    //for checking validity of status strings
    public final static List<String> ALL_STATUS = new ArrayList<>(Arrays.asList(NEW_STATUS, READY_STATUS,
									   READY_SUSPENDED_STATUS,
									   RUNNING_STATUS, BLOCKED_STATUS,
									   BLOCKED_SUSPENDED_STATUS, EXIT_STATUS));



    //Global Methods
    public static Process getProcess(int id) {
	System.out.println("get process");
	for (int i = 0; i < GlobalVariables.processes.size(); i++) {
	    System.out.println(GlobalVariables.processes.get(i).getId());
	    if (GlobalVariables.processes.get(i).getId() == id)
		return GlobalVariables.processes.get(i);
	}
	System.out.println("returning null");
	return null;
    }

    /*
     * Returns true if process deleted, false if process didn't exist
     */
    public static boolean deleteProcess(int id) {
        for (int i = 0; i < GlobalVariables.processes.size(); i++) {
	    if (GlobalVariables.processes.get(i).getId() == id) {
	        GlobalVariables.processes.remove(i);
		return true;
	    }
	}
	return false;
    }

    public static int getProcessCount() {
	return GlobalVariables.processes.size();
    }

    public static Process getProcessByIndex(int index){
	return GlobalVariables.processes.get(index);
    }

    public static Process removeQueueTail(Queue<Process> q){
	int size = q.size();
	for(int i = 0; i < size-1; i++){
	    q.offer(q.poll());
	}
	return q.poll();
    }

    /*
     * Helper method for I/O Exceptions when writing to a file
     */
    public static void write(String s) {
	if (writer == null){
	    try {
		switch(GlobalVariables.SCHED_POLICY){
		case GlobalVariables.FCFS:
		    GlobalVariables.writer = new BufferedWriter(new FileWriter("resultsFCFS.txt"));
		    break;
		case GlobalVariables.SRT:
		    GlobalVariables.writer = new BufferedWriter(new FileWriter("resultsSRT.txt"));
		    break;
		case GlobalVariables.RR:
		    GlobalVariables.writer = new BufferedWriter(new FileWriter("resultsRR.txt"));
		    break;
		default:
		    GlobalVariables.writer = new BufferedWriter(new FileWriter("resultsFCFS.txt"));
		    break;
		}
	    } catch (IOException ex) {
		return;
	    }
	}
	try {
	    System.out.println("writing " + s);
	    writer.write(s + "\n");
	} catch (IOException ex) {
	    System.out.println(ex);
	}
    }
}
