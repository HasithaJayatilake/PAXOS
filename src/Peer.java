import java.io.*;
import java.net.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Peer extends Thread {
    // private ExecutorService pool;
    private Server server;
    private PeerThread listener;
    private BufferedReader br;
    protected String username;
    private float peerID;
    private Character memberType;
    private boolean verbose;

    private float proposalNum; // Global proposal number ensures fairness
    private boolean proposalStarted;
    private boolean proposalComplete;
    private boolean faulty;
    // private String proposalStatus;

    // Constructors
    public Peer(String username, ThreadGroup threadGroup, boolean faulty) {
        super(threadGroup, username);
        verbose = false;
        try {
            String ipAddress = "230.0.0.0";
            InetAddress group = InetAddress.getByName(ipAddress);
            int port = 4567;
            this.server = new Server(group, port);
        } catch (Exception e) {
            e.printStackTrace();
        }

        this.username = username;
        this.peerID = Character.getNumericValue(username.charAt(1)) / 10f;
        this.proposalNum = 1;
        this.proposalStarted = false;
        this.proposalComplete = false;
        this.faulty = faulty;
    }

    public Peer(Server server, BufferedReader br, String username, Character memberType) {
        this.server = server;
        this.br = br;
        this.username = username;
        this.peerID = Character.getNumericValue(username.charAt(1)) / 10f;
        this.memberType = memberType;
        this.proposalNum = 1;
        this.proposalStarted = false;
        this.proposalComplete = false;
    }

    public void run() {
        try {
            PeerThread listener = new PeerThread(this, this.username, false);
            this.setListener(listener);
            if (this.faulty){
                listener.setFaulty(true);
            }
            listener.start();
        } catch (Exception e) {
            e.printStackTrace();
            interrupt();
        }
    }

    public static void main(String[] args) throws Exception {
        final String ipAddress = "230.0.0.0";
        ;
        final InetAddress group = InetAddress.getByName(ipAddress);
        final int port = 4567;
        String username = "user";
        Character memberType = 'o';

        // Create server object
        Server server = new Server(group, port);
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        boolean getInput = true;

        System.out.println("\n>> Welcome to the PaxosCity Council Comms Console!");
        System.out.println("\n----------------------------------------------------------\n");

        while (getInput) {
            System.out.println("Enter member name to connect to the members portal (ex. M6)");
            String[] setupValues = br.readLine().split(" ");
            username = setupValues[0];
            int userNum = 0;

            try {
                String[] inputChars = username.split("(?!^)");
                userNum = Integer.parseInt(inputChars[1]);

                if (userNum > 9 || inputChars.length > 2) {
                    System.out.println("\n>> Invalid member name, try again!\n");
                    continue;
                }

                switch (userNum) {
                    case 1:
                        memberType = 'a';
                        break;
                    case 2:
                        memberType = 'b';
                        break;
                    case 3:
                        memberType = 'c';
                        break;
                    default:
                        memberType = 'd';
                        break;
                }

                getInput = false;
            } catch (Exception e) {
                System.out.println("\n>> Invalid member name, try again!\n");
            }
        }

        // Instantiate peer and communicate
        Peer paxosClient = new Peer(server, br, username, memberType);
        PeerThread listener = new PeerThread(paxosClient, paxosClient.username, true);
        paxosClient.setListener(listener);
        paxosClient.communicate();

    }

    // Communication method
    public void communicate() throws Exception {

        System.out.println("\n----------------------------------------------------------\n");
        System.out.println(">> Hi " + this.username + "\n");
        System.out.println(">> You are now connected to the PaxosCity Council Comms Portal");
        // this.pool.execute(listener);
        this.listener.start();
        System.out.println(">> Listening for incoming communication ...\n");
        // String message;

        boolean runPortal = true;

        // For members 1 through 3, present option to send proposal
        if (!memberType.equals('d')) {
            while (runPortal) {

                // Check if there is an active proposal being processed by the group
                if (this.proposalStarted && (!this.proposalComplete)) {
                    if (listener.getProposerPhase().equals(PeerThread.phase3)) {
                        this.proposalComplete = true;
                    }
                    continue;
                }

                System.out.println(">> The current president is " + this.listener.getPresident() + "\n");
                System.out.println("Choose an option:\n");
                System.out.println("1. Broadcast new proposal to run for president");
                System.out.println("2. Exit\n");
                System.out.print("Option number: ");
                try {
                    int userInput = Integer.parseInt(br.readLine());

                    if (userInput > 2) {
                        System.out.println("\n>> Invalid input, try again!\n");
                        continue;
                    } else if (userInput == 1) {
                        sendProposal(false);
                    } else {
                        runPortal = false;
                        System.exit(0);
                        break;

                    }
                } catch (Exception e) {
                    System.out.println("\n>> Invalid input, try again!\n");
                    continue;
                }

            }

        }
    }

    public void sendProposal(boolean faulty) {
        String messageType = "proposal";
        this.proposalNum += this.peerID;
        String outputMessage = this.username + ":" + messageType + "-" + Float.toString(this.proposalNum);
        listener.setProposalValue(this.username);
        listener.setUpdateRequired(false);
        listener.setProposerPhase(PeerThread.phase1);
        server.sendMessage(outputMessage, messageType, listener);
        this.proposalStarted = true;
        System.out.println(">> " + this.username + ": Successfully created proposal of value " + this.username);
        listener.setActiveProposalId(this.proposalNum);
        System.out.println(">> " + this.username + ": Listening for responses from the members...");
        this.proposalNum++;
    }

    // Getters and Setters
    public Server getServer() {
        return server;
    }

    public void setListener(PeerThread listener) {
        this.listener = listener;
    }

    public PeerThread getListener() {
        return listener;
    }

    public void setProposalNum(float proposalNum) {
        this.proposalNum = proposalNum;
    }

    public float getProposalNum() {
        return proposalNum;
    }
}
