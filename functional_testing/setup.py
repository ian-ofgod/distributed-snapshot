import subprocess, time, shutil
from subprocess import PIPE
from env import *

def initial_setup(app_list, n_nodes, port_seed, out_file, oilAmount):

    ### CLEAN storage_folder
    try:
        shutil.rmtree('storage_folder')
    except:
        pass

    ### LAUNCH ###
    for i in range(0, n_nodes):
        app_list.append(subprocess.Popen([java_path, r'-jar', jar_path, str(i)], stdout=out_file, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0))
        time.sleep(0.05)
        app_list[i].stdin.write(bytes("initialize, localhost, " + str(port_seed + i) + ", " + str(oilAmount) + "\n", 'utf-8'))
        time.sleep(0.05)
        if i != 0:
            app_list[i].stdin.write(bytes("join, localhost, " + str(port_seed) + "\n", 'utf-8'))
            time.sleep(1)
        app_list[i].stdin.flush()
        print("Started, initialized and join with process " + str(i))
    time.sleep(10)

def create_app(out_file, port, id, oilAmount):
    new_app = subprocess.Popen([java_path, r'-jar', jar_path, str(id)], stdout=out_file, stdin=PIPE, stderr=subprocess.STDOUT, bufsize=0)
    time.sleep(0.05)
    new_app.stdin.write(bytes("initialize, localhost, " + str(port) + ", " + str(oilAmount) + "\n", 'utf-8'))
    new_app.stdin.flush()
    time.sleep(0.05)
    return new_app
