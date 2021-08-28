import subprocess, time
from subprocess import PIPE, STDOUT

from env import *

oilAmount = 1000
apps = []
max_nodes = 20

with open('outfile', 'w') as outfile_w:
    for i in range(0, max_nodes):
        apps.append(subprocess.Popen([java_path, r'-jar', jar_path, str(i)], stdout=outfile_w, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0))

for i in range(0, max_nodes):
    apps[i].stdin.write(bytes("initialize, localhost, " +  str(10000+i) + ", " + str(oilAmount) + "\n", 'utf-8'))

for i in range(1, max_nodes):
     apps[i].stdin.write(bytes("join, localhost, " +  str(10000) + "\n", 'utf-8'))

with open('outfile', 'r') as outfile_r:
    while True:
        line = outfile_r.readline()
        if not line:
            time.sleep(1)
        else:
            print(line, end="")

# useful resource
# https://eli.thegreenplace.net/2017/interacting-with-a-long-running-child-process-in-python/