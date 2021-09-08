from random import randrange
from setup import *

max_nodes = 40
initial_port = 10000
apps = []
oilAmount= 1000
outfile_w = open('outfile', 'w')

initial_setup(app_list=apps, port_seed=initial_port, n_nodes=max_nodes, out_file=outfile_w, oilAmount= oilAmount)

### SNAPSHOT ###
i = randrange(max_nodes)
apps[i].stdin.write(b"snapshot\n")
apps[i].stdin.flush()
print("Snapshot command sent to process " + str(i))
time.sleep(5)

### DISCONNECT ###
i = randrange(max_nodes)
apps[i].stdin.write(b"disconnect\n")
apps[i].stdin.flush()
print("Disconnected node " + str(i))
time.sleep(5)

### CRASH ###
i = randrange(max_nodes)
apps[i].kill()
time.sleep(10)

### RESTORE ###
apps[i] = subprocess.Popen([java_path, r'-jar', jar_path, str(i)], stdout=outfile_w, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0)
time.sleep(0.1)
apps[i].stdin.write(bytes("initialize, localhost, " + str(10000 + i) + ", " + str(oilAmount) + "\n", 'utf-8'))
time.sleep(0.1)
apps[i].stdin.write(b"restore\n")
apps[i].stdin.flush()
print("Restore command sent to process " + str(i))
time.sleep(10)

### DISCONNECT ###
for i in range(0, max_nodes):
    apps[i].stdin.write(b"disconnect\n")
    print("Disconnect command sent to process " + str(i))
    time.sleep(1)

### KILL PROCESSES ###
print("Sleeping...")
time.sleep(1)
for i in range(0, max_nodes):
    apps[i].kill()
    print("Killed process " + str(i))
    time.sleep(0.1)

outfile_w.close()

with open('outfile', 'r') as outfile_r:
    lines = outfile_r.readlines()
    for line in lines:
        print(line, end="")

# useful resource
# https://eli.thegreenplace.net/2017/interacting-with-a-long-running-child-process-in-python/
