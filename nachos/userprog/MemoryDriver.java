package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.*;

public class MemoryDriver {

	public MemoryDriver() {
		for(int i = 0; i < Machine.processor().getNumPhysPages(); i++) {
			freePageList.add(i);
		}
	}

	public int alloc(UserProcess proc) {
		if(!memdrvLock.isHeldByCurrentThread())
			Lib.assertNotReached("Lock must be acquired first");
		if(freePageList.isEmpty())
			return -1;
		int p = freePageList.poll();
		if(allocation.containsKey(proc)) {
			allocation.get(proc).add(p);
		} else {
			ArrayList<Integer> pl = new ArrayList<Integer>();
			pl.add(p);
			allocation.put(proc, pl);
		}
		return p;
	}

	public void free(UserProcess proc, int page) {
		if(!memdrvLock.isHeldByCurrentThread())
			Lib.assertNotReached("Lock must be acquired first");
		if(!allocation.containsKey(proc))
			Lib.assertNotReached("Process does not exist");
		if(freePageList.contains(page))
			Lib.assertNotReached("Freeing a freed page");
		allocation.get(proc).remove(page);
		freePageList.addLast(page);
	}

	public void freeAll(UserProcess proc) {
		if(!memdrvLock.isHeldByCurrentThread())
			Lib.assertNotReached("Lock must be acquired first");
		if(!allocation.containsKey(proc))
			return ;
		freePageList.addAll(allocation.get(proc));
		allocation.remove(proc);
	}

	public void acquire() {
		memdrvLock.acquire();
	}

	public void release() {
		memdrvLock.release();
	}

	private Lock memdrvLock = new Lock();
	private LinkedList<Integer> freePageList = new LinkedList<Integer>();
	private HashMap<UserProcess, ArrayList<Integer>> allocation = new HashMap<UserProcess, ArrayList<Integer>>();
}