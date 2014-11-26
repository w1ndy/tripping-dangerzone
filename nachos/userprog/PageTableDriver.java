package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.*;

public class PageTableDriver {
	public PageTableDriver(UserProcess proc) {
		this.proc = proc;
		int numPhysPages = Machine.processor().getNumPhysPages();
		entries = new TranslationEntry[numPhysPages];
		for(int i = 0; i < numPhysPages; i++) {
			entries[i] = new TranslationEntry(i, i, false, false, false, false);
		}
	}

	public void map(int vadr, int padr) {
		entries[vadr] = new TranslationEntry(vadr, padr, true, false, false, false);
	}

	public int alloc(boolean readonly) {
		UserKernel.memdrv.acquire();
		int p = UserKernel.memdrv.alloc(proc);
		UserKernel.memdrv.release();
		if(p == -1) 
			return -1;
		entries[next_vaddr] = new TranslationEntry(next_vaddr, p, true, readonly, false, false);
		next_vaddr++;
		return next_vaddr - 1;
	}

	public void destroy() {
		UserKernel.memdrv.acquire();
		UserKernel.memdrv.freeAll(proc);
		UserKernel.memdrv.release();
	}

	public int translate(int vadr) {
        int vpn = vadr / pageSize, poff = vadr % pageSize;
        int ppn = find(vpn);
        if(ppn == -1)
            return -1;
        return ppn * pageSize + poff;
	}

	public int find(int vpn) {
		if(vpn < 0 || vpn > Machine.processor().getNumPhysPages())
			return -1;
		return entries[vpn].valid ? entries[vpn].ppn : -1;
	}

	public TranslationEntry[] getPageTable() {
		return entries;
	}

	private TranslationEntry[] entries;
	private UserProcess proc;
	private int next_vaddr = 0, pageSize = Processor.pageSize;
}