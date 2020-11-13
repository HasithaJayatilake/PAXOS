import java.net.*;

public class PeerThread extends Thread {
    // The required number for majority
    private static final int majority = 5;

    // The four phases of which the peer can be in one
    public static final String phase0 = "ready";
    public static final String phase1 = "promise-phase";
    public static final String phase2 = "accept-phase";
    public static final String phase3 = "decision=phase";

    // Two phase variables since a peer can simultaneously be both
    private String proposerPhase; // proposerPhase can only be one of the four above
    private String acceptorPhase; // acceptorPhase can only be one of the four above as well.

    private Peer peer;
    private Server server;
    private InetAddress group;
    private int port;
    private String memberName;
    private float promiseNum; // Highest proposal ID that was commited to
    private float phase2AMaxProposalNum; // Highest proposal number returned by acceptors in phase 1b (for consideration
                                         // in phase 2a)
    private String paxosLeader; // Id of the currently elected proposer
    private float activeProposalId; // The proposal number for the latest proposal sent out by this peer
    private int prepareOkCount; // Number of prepare-ok messages received
    private int acceptOkCount; // Number of accept-ok messages received
    private String proposalValue; // Value that proposer has decided to send on the accept-request.
    private String updatedProposalValue; // Name of the updated member chosen during phase1
    private boolean updateRequired; // Boolean for prepare-ok counter to know if an older value was sent to be used
                                    // instead
    private String president; // Value accepted by Paxos in phase 2b
    private boolean verbose;
    private boolean keepAlive;
    private boolean faulty;


    // Constructor
    public PeerThread(Peer peer, String memberName, boolean verbose) throws Exception {
        this.verbose = verbose;
        this.peer = peer;
        this.server = peer.getServer();
        this.group = this.server.getGroup();
        this.port = this.server.getPort();
        this.memberName = memberName;
        this.promiseNum = 0; // Proposals always start at 1, so this is initialized to 0
        this.prepareOkCount = 0;
        this.acceptOkCount = 0;
        this.paxosLeader = "unassigned";
        this.president = "undecided";
        this.proposerPhase = phase0;
        this.acceptorPhase = phase0;
        this.phase2AMaxProposalNum = 0; // Proposals always start at 1, so this is initialized to 0
        this.keepAlive = true;
        this.faulty=false;
    }

    // Thread run method keeping the peer alive
    public void run() {
        try {
            byte[] buffer = new byte[1024];
            MulticastSocket socket = new MulticastSocket(this.port);
            socket.joinGroup(this.group);

            while (keepAlive) {

                if ((this.proposerPhase.equals(phase1)) && (this.prepareOkCount >= majority)) {
                    // Majority prepare ok received, send accept-request
                    if (updateRequired) {
                        this.proposalValue = this.updatedProposalValue;
                    }
                    System.out.println(">> " + this.memberName + ": Prepare-Ok majority achieved!");
                    System.out.println(">> " + this.memberName +": Starting Phase2. Sending accept-requests with value: " + this.proposalValue);
                    this.proposerPhase = PeerThread.phase2;
                    String outputMessageType = "acceptRequest";
                    // Outgoing message schema: senderID:messageType-IdOfProposalSent-value
                    String outputmsg = this.memberName + ":" + outputMessageType + "-" + this.activeProposalId + "-"
                            + this.proposalValue;
                    if (this.faulty){
                        this.server.sendMessage(outputmsg, outputMessageType, this);
                        keepAlive = false;
                    }else{
                        this.server.sendMessage(outputmsg, outputMessageType, this);
                    }
                }

                if ((this.proposerPhase.equals(phase2)) && (this.acceptOkCount >= majority)) {
                    if (this.proposalValue.equals(this.president)) {
                        System.out.println(">> " + this.memberName + ": Accept-Ok majority achieved!");
                        System.out.println(
                            ">> " + this.memberName +": Sending decide message. Paxos has agreed on value: " + this.proposalValue + "\n");

                        this.proposerPhase = PeerThread.phase3;
                        String outputMessageType = "decide";
                        // Outgoing message schema: senderID:messageType-IdOfProposalSent-value
                        String outputmsg = this.memberName + ":" + outputMessageType + "-" + this.activeProposalId + "-"
                                + this.proposalValue;
                        this.server.sendMessage(outputmsg, outputMessageType, this);
                    } else {
                        System.out.println(">> " + this.memberName + ": " + "Paxos protocol error E0!");
                    }

                }

                if (this.acceptorPhase.equals(PeerThread.phase3)) {
                    yield();
                }

                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                String msg = new String(packet.getData(), packet.getOffset(), packet.getLength());
                String senderName = msg.split(":")[0];
                String msgContent = msg.split(":")[1];

                if (keepAlive){
                    processMessage(senderName, msgContent);
                }
            }

            socket.close();

        } catch (Exception e) {

            e.printStackTrace();
            System.exit(1);
        }

    }

    // Method to process incoming messages
    public void processMessage(String sender, String message) {
        String[] msgArr = message.split("-");
        String inputMessageType = msgArr[0];
        String token = msgArr[1];
        float proposalNum;
        String intendedRecipient;
        String incomingValue;

        switch (inputMessageType) {
            case "proposal":
                if (verbose) {
                    System.out.print(">> " + this.memberName +": Proposal received:");
                    System.out.print(message + "\n");
                }

                // If incoming message is a proposal
                this.acceptorPhase = PeerThread.phase1;
                proposalNum = Float.parseFloat(token);
                if (!sender.equals(this.memberName)) {
                    this.peer.setProposalNum(proposalNum + 2);
                }

                if (proposalNum > promiseNum) {
                    // Update promise number
                    String outputMessageType = "prepareOk";
                    String outputmsg;
                    String proposalNumberString;
                    String outputValue;
                    if (sender.equals(this.paxosLeader)) {
                        outputValue = "undecided";
                    } else {
                        outputValue = this.president;
                    }

                    if ((!sender.equals(this.memberName)) && verbose) {
                        System.out.println(">> " + this.memberName +": Incoming proposal num is higher than promise. Sending Prepare-Ok ...");
                    }

                    // Check if proposer is unassigned or if it is the currently elected proposer
                    if ((this.president.equals("undecided"))) {
                        this.promiseNum = proposalNum;
                        proposalNumberString = Float.toString(this.promiseNum);

                    } else {
                        // Send prepare-ok with the old value and
                        // with the proposal number associated with the old value
                        proposalNumberString = Float.toString(this.promiseNum);
                        this.promiseNum = proposalNum;
                    }

                    // Outgoing message schema:
                    // senderID:messageType-intendedRecipient-proposalNum-value
                    outputmsg = this.memberName + ":" + outputMessageType + "-" + sender + "-" + proposalNumberString
                            + "-" + outputValue;
                    if (verbose) {
                        System.out.println(
                            ">> " + this.memberName +": Sending prepare-ok of value: " + outputValue);
                    }
                    
                    this.server.sendMessage(outputmsg, outputMessageType, this);
                } else {
                    String outputMessageType = "NACK";
                    String outputmsg;
                    String proposalNumberString;
                    String outputValue;

                }
                break;

            case "prepareOk":
                if (verbose) {
                    System.out.println(">> " + this.memberName +": Prepare-Ok received: " + message);
                }

                // Incoming message schema:
                // senderID:messageType-intendedRecipient-proposalNum-value
                intendedRecipient = token;
                float selectedProposalNum = Float.parseFloat(msgArr[2]);
                incomingValue = msgArr[3];
                // If incoming message is a prepare-ok
                // And is intended for this peer, and this peer is in phase1
                if ((this.proposerPhase.equals(PeerThread.phase1)) && intendedRecipient.equals(this.memberName)) {
                    // Increment prepareOk count
                    this.prepareOkCount += 1;
                    if (!incomingValue.equals("undecided")) {
                        System.out.println(">> Prepare-Ok received:");
                        System.out.println(">> " + message);
                        if (selectedProposalNum > this.phase2AMaxProposalNum) {
                            // Set proposal value sent during acceptance phase,
                            // To that associated with the highest proposal number sent back
                            this.phase2AMaxProposalNum = selectedProposalNum;
                            this.updateRequired = true;
                            this.updatedProposalValue = incomingValue;
                        }
                    }
                }
                break;

            case "acceptRequest":
                this.acceptorPhase = PeerThread.phase3;
                if (verbose) {
                    System.out.println(">> " + this.memberName + ": Accept-request received: " + message);
                    System.out.println();
                }

                // Incoming message schema: senderID:messageType-IdOfProposalSent-value
                proposalNum = Float.parseFloat(token);
                incomingValue = msgArr[2];
                String outputmsg;
                String outputMessageType;
                if (proposalNum == promiseNum) {
                    if (verbose){
                        System.out.println(">> " + this.memberName + ": Setting president to: " + incomingValue);
                    }
                    this.president = incomingValue;
                    outputMessageType = "acceptOk";
                    if ((!sender.equals(this.memberName)) && verbose) {
                        System.out.println(">> " + this.memberName + ": Incoming proposal num matches promise. Sending Accept-Ok ...");
                    }

                    String outputValue;
                    if (sender.equals(this.paxosLeader)) {
                        outputValue = "undecided";
                    } else {
                        outputValue = this.president;
                    }

                    // Outgoing message schema: senderID:messageType-intendedRecipient-value
                    outputmsg = this.memberName + ":" + outputMessageType + "-" + sender + "-" + outputValue;
                    this.server.sendMessage(outputmsg, outputMessageType, this);
                    this.paxosLeader = sender;
                    if (verbose) {
                        System.out.println(">> " + this.memberName +": Set Paxos leader to " + this.paxosLeader);
                    }

                } else {
                    outputMessageType = "acceptReject";
                    // Outgoing message schema: senderID:messageType-intendedRecipient-value
                    outputmsg = this.memberName + ":" + outputMessageType + "-" + sender + "-" + "null";
                }
                this.server.sendMessage(outputmsg, outputMessageType, this);

                break;

            case "acceptOk":
                // System.out.println(">> Accept-Ok received:");
                // System.out.println(">> " + message);
                // Incoming message schema: senderID:messageType-intendedRecipient-value
                intendedRecipient = token;
                // If incoming message is a accept-ok
                // And is intended for this peer, and this peer is in phase2
                if ((this.proposerPhase.equals(PeerThread.phase2)) && intendedRecipient.equals(this.memberName)) {
                    // Increment prepareOk count
                    this.acceptOkCount += 1;
                }
                break;

            case "acceptReject":
                // Do nothing
                break;

            case "decide":
                // Outgoing message schema: senderID:messageType-IdOfProposalSent-value
                incomingValue = msgArr[2];
                if (incomingValue.equals(this.president)) {
                    if (!sender.equals(this.memberName)) {
                        System.out.println(">> " + this.memberName + ": Paxos has agreed on value: " + this.president);
                    }
                } else {
                    System.out.println(">> " + this.memberName + ": Paxos protocol error E1!");
                }
                this.acceptorPhase = PeerThread.phase3;
                break;
        }
    }

    // Accessors and Mutators
    public void setPrepareOkCount(int prepareOkCount) {
        this.prepareOkCount = prepareOkCount;
    }

    public void setAcceptOkCount(int acceptOkCount) {
        this.acceptOkCount = acceptOkCount;
    }

    public void setProposerPhase(String proposerPhase) {
        this.proposerPhase = proposerPhase;
    }

    public String getProposerPhase() {
        return proposerPhase;
    }

    public String getAcceptorPhase() {
        return acceptorPhase;
    }

    public void setActiveProposalId(float activeProposalId) {
        this.activeProposalId = activeProposalId;
    }

    public float getActiveProposalId() {
        return activeProposalId;
    }

    public void setProposalValue(String proposalValue) {
        this.proposalValue = proposalValue;
    }

    public String getProposalValue() {
        return proposalValue;
    }

    public String getPresident() {
        return president;
    }

    public void setUpdateRequired(boolean updateRequired) {
        this.updateRequired = updateRequired;
    }

    public void setKeepAlive(boolean keepAlive){
        this.keepAlive = keepAlive;
    }

    public void setFaulty(boolean faulty) {
        this.faulty = faulty;
    }

    
}