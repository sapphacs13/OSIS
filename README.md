# Independent Study on Operating Systems
##### Authors: Mary DuBard and Sappha O'Meara

#### Description
This project simulates the memory management system and the scheduler of an operating system.

### Memory Manager
Our memory manager uses paging to manage the storage of processes. We simulate page replacement for the Least Recently Used (LRU) and First In First Out (FIFO) algorithms. The default page replacement policy is LRU.

If you wish to **change the page replacement policy**: Please change line 10 of  OS.java to set the POLICY String to "FIFO" or "LRU".

See *Assumptions and Comments Section* at bottom for any questions/concerns

### Scheduler
We simulate scheduling for First Come First Serve (FCFS), Shortest Remaining Time (SRT), and Round Robin (RR) algorithms. The default scheduling is FCFS.

If you wish to **change the scheduling policy**: Please change line 34 of GlobalVariables.java to set SCHED_POLICY to either FCFS, SRT, or RR.

See *Assumptions and Comments Section* at bottom for any questions/concerns

## Files Included
* Driver.java
  * Creates an instance of the OS and runs it.
* OS.java
  * Creates an instance of the memory manager, an instance of the generator, and generates a new batch.
  * It then simulates scheduling the generated batch from the given scheduling policy and page replacement policy.
* GlobalVariables.java
  * Creates certain variables and methods that should be accessible to all areas of the project.
  * e.g. the round robin quantum, the page length, the page alphabet, etc.
* Generator.java
  * Generates a new batch of processes and pages from the parameters batch time and average inter arrival (similar to assignment 4)
* PageTableRow.java
  * Creates a structure for a row in the page table.
  * Contains methods to get and set information from/to a given row.
* Process.java
  * Creates a structure for a process.
  * Includes variables such as the arrival time of a process, the page IDs that the process owns, the status of the process, etc.
* MemoryManager.java
  * Creates (and initializes) the memory (main memory and disk) and the page table.
  * Includes methods to call a page, and to remove processes from main and the disk.
  * Also includes helper methods such as checking if there is room in main memory, expanding the disk (to treat it as infinite), and placing and replacing a page in main memory (using the given page replacement policy)
* resultsFCFS.txt, resultsSRT.txt, and resultsRR.txt
  * Summarizes the results of each scheduling algorithm.

## Assumptions and Comments
 * For simplicity and ease of coding, we assume (force it to be so) that we will never call the last character (last page ID and last index) of any process until the last time unit, when we will always call the last character.
* A newly started process runs for one unit of time.
  * This is assuming negligible overhead to choose the process, as we did in class.
* It is okay that after generating a set of processes, we cannot run them on several different scheduling algorithms.
  * In practice, we would only use one algorithm at a time.
  * For testing multiple algorithms, see the above *Scheduler* section.
  * We made this assumption to decrease the number of variables that would need to be reset and the number of temporary variables that would need to be created.
* Similarly, we cannot run a batch on several different page replacement algorithms at once.
  * For testing multiple algorithms, see the *Memory Manager* section above.
* It is okay if the batch finishes before the batch time.
  * This assumption is made so we do not overly complicate the algorithm for generating a batch.
  * **Note:** We run the OS for an extra 16 time units after the batch service time, to account for the possibility of processes waiting on I/O. So we will always be able to see every process exit.