import os
from os import listdir
from os.path import isfile, join
import sys
import subprocess

DEFAULT_BENCH_DIR = "./MainDir/Bench1"

# onlyfiles = [f for f in listdir(DEFAULT_BENCH_NAME+"/player1") if isfile(join(DEFAULT_BENCH_NAME+"/player1", f))]
# print(onlyfiles)
#
# maps = [f for f in listdir(DEFAULT_BENCH_NAME+"/player1") if isfile(join(DEFAULT_BENCH_NAME+"/player1", f))]
# onlyfiles = ['client.jar']

def main():
    # arg0 - DEFAULT, script dir, arg1 - bench name (e.g. Bench1)
    args = sys.argv
    for arg in args:
        print("arg: "+arg)
    bench_name = DEFAULT_BENCH_DIR
    if len(args)>1:
        bench_name=args[1]


    maps = [f for f in listdir(bench_name+"/maps") if isfile(join(bench_name+"/maps", f))]
    maps = [bench_name + s for s in maps]
    if len(maps)==0:
        maps = [f for f in listdir( "./MapDir") if isfile(join( "./MapDir", f))]
        maps = [bench_name + s for s in maps]

    player1 = [f for f in listdir(bench_name+"/player1") if isfile(join(bench_name+"/player1", f))][0]
    player2 = [f for f in listdir(bench_name+"/player2") if isfile(join(bench_name+"/player2", f))][0]

    # Redundant
    os.environ["Test"]="sometest"
    from subprocess import check_output
    print(check_output("echo %Test%", shell=True).decode())
    # Redundant

    for map in maps:
        os.environ["AICMap"]=map
        subprocess.Popen(["java -jar ", "slave.py"] + sys.argv[1:])
        subprocess.Popen(["python", "slave.py"] + sys.argv[1:])
        print(check_output("java -jar server.jar", shell=True).decode())



main()