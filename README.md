# distributed-snapshot
A Java library to perform snapshots in a distributed environment using the Chandy-Lamport algorithm. 

The backend maintains a mesh network topology of the nodes connected.

## How to use the demo:
- Run maven:
  - clean
  - package
- Execute the obtained .jar on each machine
- The demo consists in nodes exchanging oil to each other
- The state of the node is the current oil amount
- The basic steps to get a node up and running are:
  - initialize, myHostname, myPort, initialOilAmount
  - join, gatewayHostname, gatewayPort
  - then you can take a snapshot, restore or disconnect

