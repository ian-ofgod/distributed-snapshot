import subprocess, time
from subprocess import PIPE, STDOUT

from env import *

oilAmount = 1000

with open('outfile', 'w') as outfile_w:
    app1 = subprocess.Popen([java_path, r'-jar', jar_path, "1"], stdout=outfile_w, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0)
    app2 = subprocess.Popen([java_path, r'-jar', jar_path, "2"], stdout=outfile_w, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0)
    app3 = subprocess.Popen([java_path, r'-jar', jar_path, "3"], stdout=outfile_w, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0)

app1.stdin.write(bytes("initialize, localhost, 10000, " + str(oilAmount) + "\n", 'utf-8'))
app2.stdin.write(bytes("initialize, localhost, 10001, " + str(oilAmount) + "\n", 'utf-8'))
app3.stdin.write(bytes("initialize, localhost, 10002, " + str(oilAmount) + "\n", 'utf-8'))
app2.stdin.write(b"join, localhost, 10000\n")
app3.stdin.write(b"join, localhost, 10000\n")

app1.stdin.flush()
app2.stdin.flush()
app3.stdin.flush()

with open('outfile', 'r') as outfile_r:
    while True:
        line = outfile_r.readline()
        if not line:
            time.sleep(1)
        else:
            print(line, end="")

# useful resource
# https://eli.thegreenplace.net/2017/interacting-with-a-long-running-child-process-in-python/