# PAXOS

<h2>Overview</h2>

 - This is a simple PAXOS implementation, with 9 named peers (M1 to M9).
 - M1, 2 and 3 are allowed to make proposals (they are proposers and acceptors at the same time)
 - The rest are only acceptors

<h3>Proposal Priority</h3>

 - Each proposal has a globally unique ID.
 - This is achieved by appending the nodeId to the end of the proposal ID.
 - Proposal ID is initiated at 1 for each node,
 - And is incremeneted by 1 for every proposal it sends out,
 - And for each proposal a node receives, the node's own proposal number is updated by assigning it the value of the incoming proposalNum + 2.
 - Incrementing by 2 is to improve fairness, i.e. by reducing the likelihood that the same node will continously dominate proposals.

       
<h2>Manual testing</h2>

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

<h2>Integrated testing</h2>

1. Compile all files
    Console Command: javac *.java

2. Run IntegratedPaxosTester
    Console Command: java IntegratedPaxosTester

3. Follow console instructions and choose the required test case

<h3>Test Case Outline</h3>

1) One proposer at a time no failures (M1 proposes initially, then M2 proposes)
    >> M1 proposes, and is selected pasxos leader and also becomes president.
    >> Then M2 proposes himself for president (with a higher proposal number).
    >> M2 is selected as Paxos leader, but it is M1's value that is accepted
    >> Expected output is for paxos to agree on M1

2) One proposer at a time no failures (M1 proposes initially, once complete M2 proposes twice)
    >> Same as case one, but the second time M2 proposes himself, his value is accepted.
    >> Expected output is for paxos to agree on M2

3) One proposer with failures (M2 proposes then dies)
    >> M2 dies after sending accept-request.
    >> This is setup by killing the communication thread of M2.
    >> The expected output is for all the members except M2 to select M2 as president, and paxos leader

4) Two proposers without failures (M1 and M2 propose together)
    >> Proposal number is the sum of an integer (initialised at 1), and a float (the peerID divided by 10).
    >> i.e. Initial proposal nums for M1 and M2, are 1.1 and 1.2 respectively.
    >> If M2's proposal arrives at a peer before M1, then M1 is rejected.
    >> If M2's proposal arrives after M1, M1 gets prepare-ok, and so does M2.
        >> Since M2's proposal number is larger than M1's, M2 gets the promise. 
        >> Since both of these are an initial proposal, the value returned by the acceptors in phase 1 is 'undecided'.
        >> Hence M2 and M1 are each allowed to attach their own values to the proposal.
        >> However, M1's accept-request is rejected since M2 has the promise.
        >> Expected output is paxos agreeing on M2 for president.

5) Two proposers with failures (M1 and M2 propose together, then M2 dies)
    >> In this scenario, M2 dies after sending accept-requests.
    >> The expected outcome is that all the peer's agree on M2 as president, except M2.

6) Two proposers with failures (M1 and M2 propose together, then M2 dies, then M3 proposes)
    >> In this scenario, M2 dies after sending accept-requests.
    >> Then M3 proposes.
    >> The expected outcome is that all the peers agree on M2 as president, except M2.
