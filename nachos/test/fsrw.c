#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

int main(int argc, char **argv)
{
	char buf[256];
	write(1, "test stdout\n", 12);
	int fd = creat("test_file");
	int fdr = open("input");
	if(fd == -1) return -1;
	write(1, "fd opened  \n", 12);
	read(fdr, buf, 10);
	write(fd, buf, 10);
	close(fd);
	int fd2 = open("test_file");
	unlink("test_file");
	read(fd2, buf, 10);
	write(1, buf, 10);
	close(fd2);
	return 0;
}


