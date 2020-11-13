import java.io.*;
import java.net.*;
import java.util.LinkedList;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.lang.*;

public class IntegratedPaxosTester {
    private int quorum;
    private float runtime;
    private int messageCount;
    ThreadGroup threadGroup;
    LinkedList<Peer> peerList;
    File file1;
    File file2;
    File file3;
    File file4;
    Peer M1;
    Peer M2;
    Peer M3;
    Peer M4;
    Peer M5;
    Peer M6;
    Peer M7;
    Peer M8;
    Peer M9;

    public static void main(String[] args) {
        IntegratedPaxosTester tester = new IntegratedPaxosTester();
        try {
            tester.runIntegratedTester();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public IntegratedPaxosTester() {
        this.file1 = new File("expected-output-1.txt");
        this.file2 = new File("expected-output-2.txt");
        this.file3 = new File("expected-output-3.txt");
        this.file4 = new File("expected-output-4.txt");
        this.peerList = new LinkedList<Peer>();
        this.runtime = 0f;
        this.messageCount = 0;
    }

    public void runIntegratedTester() throws Exception {

        // Instantiating useful variables
        BufferedReader userInputReader = new BufferedReader(new InputStreamReader(System.in));
        final String ipAddress = "230.0.0.0";
        ;
        final InetAddress group = InetAddress.getByName(ipAddress);
        final int port = 4567;
        // Create server object
        Server server = new Server(group, port);

        boolean gotGroupSize = false;
        System.out.println("\nWelcome to the Integrated Tester!\n");

        while (!gotGroupSize) {
            System.out.println("Choose the paxos group size (5, 7, or 9)");
            System.out.print("Number of peers: ");
            try {
                int groupSize = Integer.parseInt(userInputReader.readLine());
                gotGroupSize = true;
                switch (groupSize) {
                    case 5:
                        this.quorum = 3;
                        break;
                    case 7:
                        this.quorum = 4;
                        break;
                    case 9:
                        this.quorum = 5;
                        break;
                    default:
                        gotGroupSize = false;
                        System.out.println("\nInvalid input! Try again");
                        throw new IOException();
                }
            } catch(NumberFormatException e){
                System.out.println("\nInvalid input! Try again");
            }catch (Exception e) {
                System.out.println("------------------------------------");
                continue;
            }

        }

        boolean run = true;
        while (run) {

            // if (this.threadGroup != null){
            //     this.threadGroup.interrupt();
            //     System.out.println("Active thread count: " + this.threadGroup.activeCount());
            //     // this.threadGroup.destroy();
            // }

            System.out.println("Each testing option below is a sequence of proposals");
            System.out.println(
                    "A diff function compares the decided paxos values of each node, \n with the expected output at the end of the series of proposals\n");
            System.out.println("Choose a testing option below\n");

            System.out.println("a) One proposer: M1");
            System.out.println("b) Two proposers simultaneously: M1 and M2");
            System.out.println("c) Three proposers simultaneously: M1, M2 and M3");
            System.out.println("d) One proposer: M2 proposes and is partitioned");
            System.out.println("e) Two proposers simultaneously: M1 and M2, then M2 is partitioned");
            System.out.println(
                    "f) Three proposers: M3 is partitioned initially, while M1, M2 propose together, then M3 recovers and proposes)");
            System.out.println("g) Exit\n");
            System.out.print("Option number: ");

            try {
                String userInput = userInputReader.readLine();
                File outputFile = new File("output.txt");
                FileWriter filewriter = new FileWriter(outputFile, false);
                BufferedWriter bufferedwriter = new BufferedWriter(filewriter);
                PrintWriter outputWriter = new PrintWriter(bufferedwriter);
                String result;
                long startTime;

                switch (userInput) {
                    case "a":
                        Init_Peers(0);
                        // Start timer
                        startTime = System.currentTimeMillis();
                        M1.sendProposal(false);
                        while (true) {
                            if (M1.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                // Calculate runtime
                                this.runtime = System.currentTimeMillis() - startTime;
                                break;
                            }
                        }

                        // TimeUnit.SECONDS.sleep(1);
                        for (Peer peer : peerList) {
                            result = peer.username + ": president-" + peer.president;
                            outputWriter.println(result);
                            this.messageCount+=peer.outgoingMessageCount;
                        }

                        outputWriter.close();

                        if (filesDiff(this.file1, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case a successful!");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>>  Runtime: " + this.runtime + "ms");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>>  MessageCount: " + this.messageCount);
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case a failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }
                        System.exit(0);

                    case "b":
                        // M1 and M2 propose simultaneously
                        Init_Peers(0);
                        // Start timer
                        startTime = System.currentTimeMillis();
                        M1.sendProposal(false);
                        M2.sendProposal(false);

                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                // Calculate runtime
                                this.runtime = System.currentTimeMillis() - startTime;
                                break;
                            }
                        }

                        // TimeUnit.SECONDS.sleep(1);

                        for (Peer peer : peerList) {
                            result = peer.username + ": president-" + peer.president;
                            outputWriter.println(result);
                            this.messageCount+=peer.outgoingMessageCount;
                        }

                        outputWriter.close();

                        if (filesDiff(this.file2, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case b successful!");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> Runtime: " + this.runtime + "ms");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> MessageCount: " + this.messageCount);
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case b failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }
                        System.exit(0);
                        break;

                    case "c":
                        // M1 and M2 propose simultaneously
                        Init_Peers(0);
                        // Start timer
                        startTime = System.currentTimeMillis();
                        M1.sendProposal(false);
                        M2.sendProposal(false);
                        M3.sendProposal(false);

                        while (true) {
                            if (M3.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                // Calculate runtime
                                this.runtime = System.currentTimeMillis() - startTime;
                                break;
                            }
                        }

                        // TimeUnit.SECONDS.sleep(1);

                        for (Peer peer : peerList) {
                            result = peer.username + ": president-" + peer.president;
                            outputWriter.println(result);
                            this.messageCount+=peer.outgoingMessageCount;
                        }

                        outputWriter.close();

                        if (filesDiff(this.file3, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case c successful!");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> Runtime: " + this.runtime + "ms");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> MessageCount: " + this.messageCount);
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case c failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }
                        System.exit(0);
                        break;

                    case "d":
                        // M2 proposes, then partitioned
                        Init_Peers(1);
                        // Start timer
                        startTime = System.currentTimeMillis();
                        M2.sendProposal(false);

                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                // Calculate runtime
                                this.runtime = System.currentTimeMillis() - startTime;
                                break;
                            }
                        }

                        // TimeUnit.SECONDS.sleep(1);

                        for (Peer peer : peerList) {
                            result = peer.username + ": president-" + peer.president;
                            outputWriter.println(result);
                            this.messageCount+=peer.outgoingMessageCount;
                        }

                        outputWriter.close();

                        if (filesDiff(this.file4, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case d successful!");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> Runtime: " + this.runtime + "ms");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> MessageCount: " + this.messageCount);
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case d failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }
                        System.exit(0);
                        break;

                    case "e":
                        // Two proposers with failures
                        Init_Peers(1);
                        // Start timer
                        startTime = System.currentTimeMillis();
                        M1.sendProposal(false);
                        M2.sendProposal(false);

                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                // Calculate runtime
                                this.runtime = System.currentTimeMillis() - startTime;
                                break;
                            }
                        }

                        for (Peer peer : peerList) {
                            result = peer.username + ": president-" + peer.president;
                            outputWriter.println(result);
                            this.messageCount+=peer.outgoingMessageCount;
                        }

                        outputWriter.close();

                        if (filesDiff(this.file4, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case e successful!");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> Runtime: " + this.runtime + "ms");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> MessageCount: " + this.messageCount);
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case e failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }

                        System.exit(0);
                        break;

                    case "f":
                        // Three proposers with failures
                        Init_Peers(2);
                        System.out.println("Test case 6 initiated");
                        System.out.println("M3 has been partitioned");
                        // Start timer
                        startTime = System.currentTimeMillis();
                        M1.sendProposal(false);
                        M2.sendProposal(false);

                        while (true) {
                            if (M2.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                break;
                            }
                        }

                        // Checkpoint for consensus
                        boolean passed = true;
                        for (Peer peer : peerList) {
                            if (!peer.username.equals("M3")) {
                                if (!peer.president.equals("M2"))
                                    passed = false;
                            }
                        }

                        if (!passed) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case f failed! Checkpoint consenus not reached.");
                            System.out.println("----------------------------------------------------------------\n");
                            System.exit(0);
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Checkpoint consenus reached!");
                            System.out.println("----------------------------------------------------------------\n");
                        }

                        // Recover M3 and send proposal
                        M3.listener.setFaulty(0); // Partition M3
                        if ((M3.listener.getFaulty() == 0) && M3.listener.getKeepAlive() && M3.listener.isAlive()) {
                            // System.out.println("M3 state = " + M3.listener.getState());
                            System.out.println("M3 has recovered!");
                        } else {
                            System.out.println("Failed to recover M3");
                            System.exit(0);
                        }
                        M3.sendProposal(false);

                        while (true) {
                            if (M3.getListener().getProposerPhase().equals(PeerThread.phase3)) {
                                // Calculate runtime
                                this.runtime = System.currentTimeMillis() - startTime;
                                break;
                            }
                        }

                        for (Peer peer : peerList) {
                            result = peer.username + ": president-" + peer.president;
                            outputWriter.println(result);
                            this.messageCount+=peer.outgoingMessageCount;
                        }

                        outputWriter.close();

                        if (filesDiff(this.file2, outputFile)) {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case f successful!");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> Runtime: " + this.runtime + "ms");
                            System.out.println(">>>>>>>>>>>>>>>>>>>>>> MessageCount: " + this.messageCount);
                            System.out.println("----------------------------------------------------------------\n");
                        } else {
                            System.out.println("\n----------------------------------------------------------------");
                            System.out.println("Test case f failed.");
                            System.out.println("----------------------------------------------------------------\n");
                        }

                        System.exit(0);
                        break;

                    case "g":
                        System.exit(0);
                        break;
                    default:
                        outputWriter.close();
                        System.out.println("\nInvalid input! Try again");
                        throw new IOException();
                }

            } catch(NumberFormatException e){
                System.out.println("\nInvalid input! Try again");
            } catch (Exception e) {
                System.out.println("\n------------------------------\n");
                continue;
            }
        }
    }

    public void Init_Peers(int failureMode) {
        this.threadGroup = new ThreadGroup("Paxos-group");
        this.peerList.clear();

        this.M1 = new Peer("M1", quorum, threadGroup, 0);
        // Start M2 contingent on failure mode
        if (failureMode == 1) {
            this.M2 = new Peer("M2", quorum, threadGroup, failureMode);
        } else {
            this.M2 = new Peer("M2", quorum, threadGroup, 0);
        }

        // Start M3 contingent on failure mode
        if (failureMode == 2) {
            this.M3 = new Peer("M3", quorum, threadGroup, failureMode);
        } else {
            this.M3 = new Peer("M3", quorum, threadGroup, 0);
        }

        this.M4 = new Peer("M4", quorum, threadGroup, 0);
        this.M5 = new Peer("M5", quorum, threadGroup, 0);

        // At least 5 peers required in every configuration
        this.peerList.add(M1);
        this.peerList.add(M2);
        this.peerList.add(M3);
        this.peerList.add(M4);
        this.peerList.add(M5);
        M1.start();
        M2.start();
        M3.start();
        M4.start();
        M5.start();

        if (this.quorum > 3){
            this.M6 = new Peer("M6", quorum, threadGroup, 0);
            this.M7 = new Peer("M7", quorum, threadGroup, 0);
            this.peerList.add(M6);
            this.peerList.add(M7);
            M6.start();
            M7.start();

            if(this.quorum>4){
                this.M8 = new Peer("M8", quorum, threadGroup, 0);
                this.M9 = new Peer("M9", quorum, threadGroup, 0);
                this.peerList.add(M8);
                this.peerList.add(M9);
                M8.start();
                M9.start();
            }
        }
    }

    public boolean filesDiff(File f1, File f2) {

        boolean diff = true;

        try {
            BufferedReader reader1 = new BufferedReader(new FileReader(f1));
            BufferedReader reader2 = new BufferedReader(new FileReader(f2));

            String line1 = null;
            String line2 = null;
            while ((diff) && ((line1 = reader1.readLine()) != null) && ((line2 = reader2.readLine()) != null)) {
                if (!line1.equals(line2)) {
                    diff = false;
                    break;
                }
            }
            reader1.close();
            reader2.close();
            return diff;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
