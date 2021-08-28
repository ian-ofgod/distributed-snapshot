import subprocess, time
from random import randrange
from subprocess import PIPE, STDOUT

from env import *

oilAmount = 1000
apps = []
max_nodes = 20

outfile_w = open('outfile', 'w')

### LAUNCH ###
for i in range(0, max_nodes):
    apps.append(subprocess.Popen([java_path, r'-jar', jar_path, str(i)], stdout=outfile_w, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0))
    print("Started process " + str(i))
    time.sleep(0.5)

### INITIALIZE ###
for i in range(0, max_nodes):
    apps[i].stdin.write(bytes("initialize, localhost, " +  str(10000+i) + ", " + str(oilAmount) + "\n", 'utf-8'))
    print("Initialize command sent to process " + str(i))
    time.sleep(0.5)

### JOIN ###
for i in range(1, max_nodes):
     apps[i].stdin.write(bytes("join, localhost, " +  str(10000) + "\n", 'utf-8'))
     print("Join command sent to process " + str(i))
     time.sleep(0.5)

for i in range(0, max_nodes):
    apps[i].stdin.flush()

### SNAPSHOT ###
print("Sleeping...")
time.sleep(5)
i = randrange(max_nodes)
apps[i].stdin.write(bytes("snapshot\n", 'utf-8'))
apps[i].stdin.flush()
print("Snapshot command sent to process " + str(i))

### RESTORE ###
print("Sleeping...")
time.sleep(5)
i = randrange(max_nodes)
apps[i].kill()
apps[i] = subprocess.Popen([java_path, r'-jar', jar_path, str(i)], stdout=outfile_w, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0)
apps[i].stdin.write(bytes("initialize, localhost, " +  str(10000+i) + ", " + str(oilAmount) + "\n", 'utf-8'))
apps[i].stdin.write(bytes("restore\n", 'utf-8'))
apps[i].stdin.flush()
print("Restore command sent to process " + str(i))

outfile_w.close()

print("Sleeping...")
time.sleep(5)
for i in range(0, max_nodes):
    apps[i].kill()
    print("Killed process " + str(i))
    time.sleep(0.1)

with open('outfile', 'r') as outfile_r:
    while True:
        line = outfile_r.readline()
        if not line:
            time.sleep(1)
        else:
            print(line, end="")

# useful resource
# https://eli.thegreenplace.net/2017/interacting-with-a-long-running-child-process-in-python/