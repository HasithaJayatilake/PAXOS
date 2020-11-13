# PAXOS

## Overview

 - This is a simple PAXOS implementation, with 9 named peers (M1 to M9).
 - M1, 2 and 3 are allowed to make proposals (they are proposers and acceptors at the same time)
 - The rest are only acceptors

### Proposal Priority

 - Each proposal has a globally unique ID.
 - This is achieved by appending the nodeId to the end of the proposal ID.
 - Proposal ID is initiated at 1 for each node,
 - And is incremeneted by 1 for every proposal it sends out,
 - And for each proposal a node receives, the node's own proposal number is updated by assigning it the value of the incoming proposalNum + 2.
 - Incrementing by 2 is to improve fairness, i.e. by reducing the likelihood that the same node will continously dominate proposals.

       
## Manual testing

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

## Integrated testing

1. Compile all files
> Console Command: javac *.java

2. Run IntegratedPaxosTester
> Console Command: java IntegratedPaxosTester

3. Follow console instructions and choose the required test case

### Test Case Outline  

The base configurations for the standard functioning mode are as follows:  

a) One proposer: M1  
b) Two proposers simultaneously: M1 and M2  
c) Three proposers simultaneously: M1, M2 and M3  

The expected consensus values of the variable ‘president’ for the configurations above are, M1, M2 and M3 respectively for configurations a), b) and c).  

The base configurations for the faulty functioning mode are the following  

d) One proposer: M2 proposes and is partitioned  
e) Two proposers simultaneously: M1 and M2, then M2 is partitioned  
f) Three proposers: M3 suffers a network partition, while M1, M2 propose together, then M3 recovers and proposes.   

In configurations d) and e), M2 dies immediately after sending accept-requests, so is unable to update their own value of ‘president.’ The expected consensus value of the variable ‘president’ for both configurations 1 and 2 in the faulty mode, is M2, despite M2 not having learned this value itself. The expected consensus value in configuration f) is M2, and M3 is expected to have learned this at the end of all the transactions.  
