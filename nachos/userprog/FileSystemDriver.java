package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.*;

public class FileSystemDriver
{
	public FileSystemDriver() {
		descriptor_to_handler.put(0, UserKernel.console.openForReading());
		descriptor_to_handler.put(1, UserKernel.console.openForWriting());
	}

	public int creat(String name) {
		OpenFile f = ThreadedKernel.fileSystem.open(name, true);
		if(f == null)
			return -1;
		descriptor_to_handler.put(fd_next, f);
		fd_next++;
		return fd_next - 1;
	}

	public int open(String name) {
		OpenFile f = ThreadedKernel.fileSystem.open(name, false);
		if(f == null)
			return -1;
		descriptor_to_handler.put(fd_next, f);
		fd_next++;
		return fd_next - 1;
	}

	public int read(int fd, byte[] data) {
		if(!descriptor_to_handler.containsKey(fd))
			return -1;
		OpenFile f = descriptor_to_handler.get(fd);
		return f.read(data, 0, data.length);
	}

	public int write(int fd, byte[] data) {
		if(!descriptor_to_handler.containsKey(fd))
			return -1;
		OpenFile f = descriptor_to_handler.get(fd);
		return f.write(data, 0, data.length);
	}

	public int close(int fd) {
		if(!descriptor_to_handler.containsKey(fd))
			return -1;
		OpenFile f = descriptor_to_handler.get(fd);
		f.close();
		descriptor_to_handler.remove(fd);
		return 0;
	}

	public int unlink(String name) {
		if(ThreadedKernel.fileSystem.remove(name))
			return 0;
		return -1;
	}

	private HashMap<Integer, OpenFile> descriptor_to_handler = new HashMap<Integer, OpenFile>();
	private int fd_next = 2;
}