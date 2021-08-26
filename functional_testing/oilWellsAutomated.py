import subprocess
from subprocess import PIPE, STDOUT

from env import *

app1 = subprocess.Popen([java_path, r'-jar', jar_path], stdout=PIPE, stdin=PIPE, stderr=STDOUT, bufsize=0)
app2 = subprocess.Popen([java_path, r'-jar', jar_path], stdout=PIPE, stdin=PIPE, stderr=STDOUT, bufsize=0)
app3 = subprocess.Popen([java_path, r'-jar', jar_path], stdout=PIPE, stdin=PIPE, stderr=STDOUT, bufsize=0)

app1.stdin.write(b"initialize, localhost, 11111, 1000\n")
app2.stdin.write(b"initialize, localhost, 11112, 2000\n")
app3.stdin.write(b"initialize, localhost, 11113, 3000\n")
app2.stdin.write(b"join, localhost, 11111\n")
app3.stdin.write(b"join, localhost, 11111\n")

# here add the commands that you wish to execute

app1.stdin.flush()
app2.stdin.flush()
app3.stdin.flush()
while True:
    print("[APP1] " + app1.stdout.readline().decode())
    print("[APP2] " + app2.stdout.readline().decode())
    print("[APP3] " + app3.stdout.readline().decode())

# TODO: there should be a better way to read lines simultaneously from all the application stdin
# useful resource
# https://eli.thegreenplace.net/2017/interacting-with-a-long-running-child-process-in-python/