# PAXOS
Simple election paxos
This README is for the normal functionality of the system, and can be used for manual testing.
For integrated testing - refer to the TESTING_README.txt file.

Proposal IDs: Each proposal has a globally unique ID. 
This is achieved by appending the nodeId to the end of the proposal ID. 
Proposal ID is initiated at 1 for each node,
And is incremeneted by 1 for every proposal it sends out,
And for each proposal a node receives, the node's own proposal number is updated 
        by assigning it the value of the incoming proposalNum + 2.
Incrementing by 2 is to improve fairness,
        i.e. by reducing the likelihood that the same node will continously dominate proposals

Running each of the peers for manual testing
1. Compile all files
    Console Command: javac *.java

2. Setup Peers for Members M1 through to M9 
    - Console Command: java Peer 
    - Follow console instructions, when prompted with member name, enter required name:
    - ExampleConsole Command: M1
    - Note: Connects to port 4567, and multicast IP Address - 230.0.0.0
    - Repeat this for each Member
    - Members 1 through 3 will be prompted with an option to send a proposal, 
        other members will simply show status updates.
