# distributed-snapshot
A Java library to perform snapshots in a distributed environment using the [Chandy-Lamport algorithm](https://dl.acm.org/doi/abs/10.1145/214451.214456). 
The backend maintains a mesh network topology of the nodes connected.

All the code of the library is commented following the javadoc directive to allow for an easy use of the project. 

## How to use the demo:
A demo is also provided in the form of nodes exchaning oil to each other. The state of the nodes is the current oil amount present. 
- Run maven:
  - clean
  - package
- Execute the obtained .jar on each machine
- The basic steps to get a node up and running are:
  1) initialize, myHostname, myPort, initialOilAmount
  2) join, gatewayHostname, gatewayPort
  3) then you can take a snapshot, restore or disconnect

